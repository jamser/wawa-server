package com.wawa.web

import com.mongodb.BasicDBObject
import com.mongodb.DBCollection
import com.wawa.AppProperties
import com.wawa.api.Web
import com.wawa.base.BaseController
import com.wawa.base.Crud
import com.wawa.base.anno.Rest
import com.wawa.common.doc.MongoKey
import com.wawa.common.doc.Result
import com.wawa.common.util.JSONUtil
import com.wawa.model.ActionTypeEnum
import com.wawa.service.MachineServerService
import org.apache.commons.lang.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.bind.ServletRequestUtils

import javax.annotation.Resource
import javax.servlet.http.HttpServletRequest

import static com.wawa.common.util.WebUtils.$$

/**
 * 公共信息接口
 */
@Rest
class PublicController extends BaseController {

    Logger logger = LoggerFactory.getLogger(PublicController.class)
    public static final String APP_ID = "wawa_default" //客户标识
    public static final String APP_SECRET = "ab75e7a2de882107d3bc89948a1baa9e" //分配给客户
    public static final String APP_TOKEN = "9c1b7a6868fd2229d1b62e719665bb0b" //给机器用
    public static final String SERVER_URI = AppProperties.get('server.domain')
    public static final String STREAM_URI = AppProperties.get('stream.domain')
    public static final String API_DOMAIN = AppProperties.get("api.domain")

    @Resource
    MachineServerService serverService

    DBCollection machine() {
        adminMongo.getCollection('machine')
    }
    DBCollection record_log() {
        logMongo.getCollection('record_log')
    }

    def blackword_list(HttpServletRequest req) {
        Integer type = Web.firstNumber(req)
        def query = $$('type', $$($in: [type, 2]))   //公共关键字 type:2
        def db_obj = adminMongo.getCollection('blackwords').find(query, $$(_id: 1)).batchSize(5000).toArray()
                .collect { it['_id'] }
        def result = [code: 1, data: db_obj]
        String json = JSONUtil.beanToJson(result)
    }

    /**
     * //todo 机器注册
     * @param req
     * @return
     */
    def machine_on(HttpServletRequest req) {
        //就是机器对应的mac地址
        def _id = ServletRequestUtils.getStringParameter(req, '_id')
        //现场工作人员为机器分配的名称
        def name = ServletRequestUtils.getStringParameter(req, 'name')
        if (StringUtils.isBlank(_id) || StringUtils.isBlank(name)) {
            return Result.error
        }
        def info = machine().findOne($$(_id: _id)) as BasicDBObject ?: new BasicDBObject()
        if (info['timestamp'] == null) {
            info['timestamp'] = System.currentTimeMillis()
        }
        if (info['name'] != name) {
            info['name'] = name
        }
        if (info['app_id'] == null) {
            info['app_id'] = APP_ID
            info['app_token'] = APP_TOKEN
        }
        if (info['server_uri'] == null) {
            info['server_uri'] = SERVER_URI
        }
        if (info['stream_uri'] == null) {
            info['stream_uri'] = STREAM_URI
        }
        def device_comport = ServletRequestUtils.getStringParameter(req, 'comport')
        if (info['comport'] != device_comport) {
            info['comport'] = device_comport
        }
        def camera1 = ServletRequestUtils.getStringParameter(req, 'camera1')
        if (info['camera1'] != camera1) {
            info['camera1'] = camera1
        }
        def camera2 = ServletRequestUtils.getStringParameter(req, 'camera2')
        if (info['camera2'] != camera2) {
            info['camera2'] = camera2
        }
        info['url'] = API_DOMAIN + 'public/machine_on'
        info['status'] = 'on'
        info['last_modify'] = System.currentTimeMillis()
        def upadte = $$(info)
        machine().update($$(_id: _id), upadte, true, false, writeConcern)
        return [code: 1, data: info]
    }

    //todo 接口都需要加密验证
    /**
     * 1 获取机器列表信息
     * @param req
     * @return
     */
    def list(HttpServletRequest req) {
        def app_id = req.getParameter('app_id')
        Crud.list(req, machine(), $$(app_id: app_id), MongoKey.ALL_FIELD, $$(order: -1)) {
            //顺便每台机器的status, 视频流地址等信息
        }
    }

    /**
     * 获取单个机器的详情，包括机器状态和视频流地址等信息
     * @param req
     */
    def info(HttpServletRequest req) {
        def device_id = req.getParameter('device_id')
        def result = serverService.send(device_id, [action: ActionTypeEnum.STATUS.name(), ts: System.currentTimeMillis()])
        [code: 1, data: result]
    }

    /**
     * 2 请求上机，成功后分配操作地址
     * 请求分配对应的机器
     * @param req
     * @return
     */
    def assign(HttpServletRequest req) {
        def app_id = req.getParameter('app_id')
        def ts = req.getParameter('ts')
        def sign = req.getParameter('sign')
        def record_id = req.getParameter('record_id') //第三方ID
        def user_id = req.getParameter('user_id') //第三方ID
        def device_id = req.getParameter('device_id') //第三方ID
        //todo 传入各种强力抓信息，有服务器来完成这个操作
        //todo 获取sign

        //todo 如果已查询到结果信息且在时间内则直接返回，未查到结果信息生成
        //如果校验通过，记录本次请求
        //service.getStatus
        //如果机器状态不对则直接返回失败

        //如果机器状态成功则记录当前结果
        //def log_id =
        Map result = serverService.send(device_id, [action: 'status', ts: System.currentTimeMillis()])
        if (result == null || result.get('code') != 1) {
            return Result.error
        }
        def status = result.get('data') as Integer
        if (status != 0) {
            return [code: 1, data: [status: status]]
        }
        //todo 记录本次上机请求的参数
        def _id = device_id + '_' + System.currentTimeMillis()
        record_log().save($$(_id: _id,timestamp: System.currentTimeMillis()))
        return [code: 1, data: [status: status, ws_url: 'ws://localhost:8887?device_id=', log_id: _id]]
    }

}