package com.zw.alipaytest.controller;

import com.alibaba.fastjson.JSON;
import com.alipay.easysdk.factory.Factory;
import com.alipay.easysdk.kernel.Config;
import com.alipay.easysdk.kernel.util.ResponseChecker;
import com.alipay.easysdk.payment.common.models.AlipayTradeRefundResponse;
import com.alipay.easysdk.payment.page.models.AlipayTradePagePayResponse;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zw.alipaytest.domain.Order;
import com.zw.alipaytest.service.OrderTService;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author zzw
 * @since 2023-04-08
 */
@RestController
@RequestMapping("/order")
@CrossOrigin(origins = "*")
public class OrderTController {

    @Resource
    private OrderTService orderTService;

    static {
        // 1. 设置参数（全局只需设置一次）
        Factory.setOptions(getOptions());
    }

    /**
     * 支付订单
     * @param orderId
     * @return
     */
    @PostMapping("/payOrder")
    public Object payOrder(@RequestBody String orderId){
        Object re=new Object();
        Map mapTypes = JSON.parseObject(orderId);
        QueryWrapper<Order> wq=new QueryWrapper<>();
        wq.eq("order_id",mapTypes.get("orderId"));
        Order order=orderTService.getOne(wq);
        try {
            // 2. 发起API调用
            AlipayTradePagePayResponse response = Factory.Payment.Page()
                    .pay(order.getName(), order.getOrderId(), order.getTotal().toString(),"http://minisailboat.cn");
            // 3. 处理响应或异常
            if (ResponseChecker.success(response)) {
                System.out.println("创建订单成功");
                re=response.getBody();
            } else {
                System.err.println("调用失败");
            }
        } catch (Exception e) {
            System.err.println("调用遭遇异常，原因：" + e.getMessage());
            throw new RuntimeException(e.getMessage(), e);
        }
        return re;
    }
    @PostMapping("/notify")  // 注意这里必须是POST接口
    public String payNotify(HttpServletRequest request) throws Exception {
        Map< String , String > params = new HashMap <> ();
        Map requestParams = request.getParameterMap();
        for(Iterator iter = requestParams.keySet().iterator(); iter.hasNext();){
            String name = (String)iter.next();
            String[] values = (String [])requestParams.get(name);
            String valueStr = "";
            for(int i = 0;i < values.length;i ++ ){
                valueStr =  (i==values.length-1)?valueStr + values [i]:valueStr + values[i] + ",";
            }
            //乱码解决，这段代码在出现乱码时使用。
            //valueStr = new String(valueStr.getBytes("ISO-8859-1"), "utf-8");
            params.put (name,valueStr);
        }
        System.out.println(params);
        //切记alipaypublickey是支付宝的公钥，请去open.alipay.com对应应用下查看。
//        boolean AlipaySignature.rsaCheckV1(Map<String, String> params, String publicKey, String charset, String sign_type);

//        boolean flag = AlipaySignature.rsaCheckV1 (params,alipaypublicKey, charset,"RSA2");
        boolean re=Factory.Payment.Common().verifyNotify(params);
        if (re){
            try {
                QueryWrapper<Order> wq=new QueryWrapper<>();
                Order or=new Order();
                or.setState("已支付");
                wq.eq("order_id",params.get("out_trade_no"));
                orderTService.update(or,wq);
            }catch (Exception e){
                e.printStackTrace();
                return "支付失败";
            }
            return "支付成功";
        }else{
            return "支付失败";
        }

    }

    /**
     * 添加订单
     * @param order
     * @return
     */
    @PostMapping("/addOrder")
    public Object addOrder(@RequestBody Order order){
        order.setOrderId(new SimpleDateFormat("yyyyMMdd").format(new Date()) + System.currentTimeMillis());
        Map<String,Object> re;
        order.setState("未支付");
        order.setTime(new Date());
        try {
            boolean s=orderTService.save(order);
            if (s){
                re=new HashMap<>();
                re.put("code",1);
                re.put("message","下单成功");
            }else{
                re=new HashMap<>();
                re.put("code",0);
                re.put("message","下单失败");
            }
        }catch (Exception e){
            e.printStackTrace();
            re=new HashMap<>();
            re.put("code",0);
            re.put("message","下单失败");
        }
        return re;
    }

    /**
     * 查询所有订单
     * @return
     */
    @GetMapping("/getOrder")
    public Object getOrder(){
        QueryWrapper<Order> wp=new QueryWrapper<>();
        wp.select("order_id", "name", "time", "state", "total");
        return orderTService.listMaps(wp);
    }
    //取消订单
    @DeleteMapping("/cancelOrder")
    public Object cancelOrder(String orderId){
        Map<String,Object> map=new HashMap<>();
        try {
            QueryWrapper<Order> wq=new QueryWrapper<>();
            Order or=new Order();
            or.setState("已取消");
            wq.eq("order_id",orderId);
            boolean s=orderTService.update(or,wq);
            if (s){
                map.put("code",1);
                map.put("message","取消成功");
            }else {
                map.put("code",0);
                map.put("message","取消失败");
            }
        }catch (Exception e){
            e.printStackTrace();
            map.put("code",0);
            map.put("message","取消失败");
        }
        return map;
    }

    /**
     * 退款
     * @param order 退款订单
     * @return
     */
    @PostMapping("/refundOrder")
    public Object refundOrder(@RequestBody Order order) {
        Map<String,Object> map=new HashMap<>();
        try {
            //查询是否重复退款
            if("REFUND_SUCCESS".equals(Factory.Payment.Common().queryRefund(order.getOrderId(), order.getOrderId()).refundStatus)){
                map.put("code",0);
                map.put("message","已退款，请勿重复操作");
            }else{
                AlipayTradeRefundResponse response = Factory.Payment.Common().refund(order.getOrderId(),Double.toString(order.getTotal()));
                if ("10000".equals(response.code)) {
                    System.out.println("退款成功");
                    QueryWrapper<Order> wq=new QueryWrapper<>();
                    Order or=new Order();
                    or.setState("已退款");
                    wq.eq("order_id",order.getOrderId());
                    orderTService.update(or,wq);
                    map.put("code",1);
                    map.put("message","退款成功");
                } else {
                    System.out.println("退款失败");
                    map.put("code",0);
                    map.put("message","退款失败");
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            map.put("code",0);
            map.put("message","退款失败");
        }

        return map;
    }

    /**
     * 删除订单
     * @param orderId
     * @return
     */
    @DeleteMapping("/delOrder")
    public Object delOrder(String orderId){
        Map<String,Object> map=new HashMap<>();
        try{
            //删除订单
            QueryWrapper<Order> wq=new QueryWrapper<>();
            wq.eq("order_id",orderId);
            boolean s=orderTService.remove(wq);
            if(s){
                map.put("code",1);
                map.put("message","删除成功");
            }else{
                map.put("code",0);
                map.put("message","删除失败");
            }
        }catch (Exception e){
            e.printStackTrace();
            map.put("code",0);
            map.put("message","删除失败");
        }
        return map;
    }
    //支付宝应用配置
    private static Config getOptions() {
        Config config = new Config();
        config.protocol = "https";
        config.gatewayHost = "openapi.alipaydev.com";
        config.signType = "RSA2";
        config.appId = "2021000122676301";
        config.notifyUrl = "http://minisailboat.cn/order/notify";
//        config.appId = "2021000122657269";
        // 为避免私钥随源码泄露，推荐从文件中读取私钥字符串而不是写入源码中
//        config.merchantPrivateKey = "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCmv46sGCW1smYetW+YBfvcWQctYIQTQfylh4HigkRybcN+SZAzMOL89Jg0NDoNn1aSavtHFMiT82bl3UHNX1+T/L0OKXt+XENerY0oJR57rqiryCEB3AWU5IF80s5pw7iRWfofM5JXSS1h1aEFA2be+So/1oXgOcapb2/7gH92TfWORiBBjkqAqXnrAJud7lt/FhgwbC3qaHO8difFZYU32MOi5NebDqZ0EtdlHbwFSqXhmwmt8CHBombRrxhkVST7hhE4wij+xmPhtatxzsEzJBRjdKPdAMZ8LEm8NPru2JkZvjQGty+UJhBSaVfUls8i0b5IGqWIGJuiRRljnB3nAgMBAAECggEANwGL3ANfNS/zdf/eHyUiL08DChoDl6K16BjqZMEaOTEyQw+bTPe8eTtqlhYAbIv6b/RwjcMyY3PwprU1Rr1GdzfxWaGfhlCDPNE9dlfAVYaJR5mOIeHlyXcTrAySYNtaoEAxPWeyx65xFQv2wQOdRMKnCEYJBa2pdi03oJyD749BDvavcYerdEy8teDBVGu8OIYZ79NxfvYV7K/1NSXXzU6zizSRFI8/JNIVYiSOSBH9iuax4gL9bQFDpQZxrlYJZeCYIRWCeltWQtys5FEgXkNTV3Mg2J6mj0e574UBZYmuUkG+LhotLKfgD7FbWse9Z3UMAKBP34rHxww2TrXGgQKBgQDkBEUPh6heEgvHUYMlmdIwH6Xa2vr+GcdYGvCYEkcpow5V9BoEK4qwxnKc2rxQuamDG1uReQ2hRCUsj+fiaknaNKlR/HXI6DBQ+anrpEguQ1UdHmRumUqbWX59DqkpDrdH5FSlISm3SFMgczVN3yhGI9ThBfqcYypFn7k+w1+FJwKBgQC7NmIKuFJnNFePiqi6JkulXz7Ct2/2mqxAiBNsR9u2+L0DjJG5H1hW3zcTBeMvNF6giB/8WUBXEaQbbiWgR6f+MwKdqDOn1koXhbESB6Sqsqm0oPuZt+fdaLq9WijTj/l7+wuOQlN3n4rWfSzHj+n95HIc3M5cAynPzRgckmGZQQKBgDiTSqNo0IDOz3dcJUM+IGXqZFeiP+audMTKomnJFzkgiR0QnSftudLU2nlK2LDa8FFkh1rYA2bBJswgxLNsFfj7WgvKrVr1KY/d8qhSMcqw0DySXu8GP/m9weG7soNKcHV5FPuH9/bPDjkd/NrehPqqR1ayChWlkUaHNAp7pYa/AoGAcgsv8F3WfN4q1Ntf6332qPf7cHx0bSJN+kWqp1Si6LRf2DCieMY6dzklAKaefZwGZP5nlb787c1mTftsWlYsZTLHziivxvdITUn40wXq7r1Fmi5S9pJMYdkxoLWylyZmkXsdz/xhgkajOgQo6iymGy81wJVV9EFNEDRdEtKtN8ECgYEA1bZmpU+Vxg6BXwEwtdXx6bMNGOi0PXmbxK1wpkDyFA+OoWfffvyg+Xqu4ARCCozejNZQORO8T0ld0/KZXneH24vc2bgonbx5gzgP0UmuGoqWnxRqPAU5qHrwubxBo/jjPpdbg7MetEErD+W4jKnrCcbrdkLJKTGocrNkh04r+do=";
        config.merchantPrivateKey = "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCBJeok4UFTR+vjSDhR/q7SS/rGxO6WUw4mc2slSnB+JXlju6H/Ta6jbHHQZcOjTgXllT7fKbWKtYDZoR6m2vCQ/q9rsVHJrG0uB0+wHWqZhffAailGSQXmfOlqNJjQsddy7aR5qEySFxTVvae+3qRCK+KXeQihixQRW1SZU3q0MTCewfsUVUdDneYKtoBhMu4qXXrO8FRifoNimtVPEx7SCmzHgH5WjvLfujrmc+sut2N66or2Yf9Gw84IhjXZaqVm0yZRjmA90zH0GqDbycAx15aZOAXmOy1xnoWknkYzB23aP6h9cdIrpLtMzmnmp/5nPrhD8qR16EBng79+mj/XAgMBAAECggEAZLjVK34iHHlzFwc0JX6jiY+IPY1JJiQQXRyW67ZqlXdUfNvRM/O5x+rDuOwk8C/hmXQUXKeC1nNH0nM+HJOUGwEGfs8EIm0/mS5Kj+fhQ2qgFoi1OAOrfl7dC9+JbvnENfXw2JDRZv5pWWlYchvhr1nalpfmi1aUiBTandnlULIPsx75nTNn/nd47snOPmFNLiEk7pLviAclDzZn/EWfr0gOGftD2Gh6jRj0qT5bDKtvWXWX/pPjWeof5I+4JiFTwBx8ivUnjwWhDygWiYtbHV/LeviMIrLtUlnEhmw8deU74GxI5hV1WQnoooco8Yyxq8YM/5CoNI6bjok5pdBfgQKBgQDGPsguvrpyG7QJHkm++C6SCA3tZqFxXcJVFUZvqS284fXjuO9Qjm1IlOnvKcRHE8ELh1ZD3Qty/iO9KrZqpI6DdO7TowkxsMCheIoiiNR/FlN2xUc16nvwj4DtROyG8qWoHLLevvRiGedkUV2koCNa4D7O+fbBTzAVTPAEpX0xvwKBgQCmxdZkxoinyd/jiyn5vwHRVpvohoVPT09UDh+roorOnr3hKHwdDf1tmZyO4aeQ0Sm7VnwHXlt3f7uLSI6eQo5ZeR+Ffeh//XrSu4x1QqP4qMjMT34uDIAhFs+L85WvNpi87+AqLZGyB7nirhvOpeTT9+ADObpLtHLtxc612CRH6QKBgHf3SBaZjqQ8xX77GC97alsuipcOE0ZSMaZhQIWTwDzcFHugzxlVhyZ0DviOsts8Rgbe+EILAsGsrx3rOgZg3GkKmvfxYj/ysS9FjXoGiWj6rrlh+ozS//t8K83pdqTXM43/B/MpZSP0fwVDA8L6sUpuBzQjKcKjU/qf22NEbfVrAoGANLImgElLPNlI6Tk71jJIfMEFGoAG93xz8HdWAoGloov+K3sDXJrjDRKQqYUb0WKF8S/umNVzyVqPA/+1MMSO/i6Liz6xjF+nw5aHVzXrYdLTP9uOXzaL9eijQ/F/xpFVyb1x82Hau4o9bibdmnpIx8F3Aw3mk60882yhWEwniqECgYEAl7sRpkW00LA6wT/JPsPjD3UZZtfUwzmbnva884MpRQbhXocUzUdQ3tLVok4GmXqsY+uvh7kDwOnbfTNNyFdeiqeuodu4R4ULLEBqnl/XWTZMYJvskygY18YahQDtwhgx/6Hn3qmYGMsK8qbkSUHjQQQ3CdlwytPtmTXx3ykRStE=";
//            //注：证书文件路径支持设置为文件系统中的路径或CLASS_PATH中的路径，优先从文件系统中加载，加载失败后会继续尝试从CLASS_PATH中加载
//            config.merchantCertPath = "<-- 请填写您的应用公钥证书文件路径，例如：/foo/appCertPublicKey_2019051064521003.crt -->";
//            config.alipayCertPath = "<-- 请填写您的支付宝公钥证书文件路径，例如：/foo/alipayCertPublicKey_RSA2.crt -->";
//            config.alipayRootCertPath = "<-- 请填写您的支付宝根证书文件路径，例如：/foo/alipayRootCert.crt -->";

        //注：如果采用非证书模式，则无需赋值上面的三个证书路径，改为赋值如下的支付宝公钥字符串即可
//        config.alipayPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAixFHu5KsSSsg4AoWn9G/GS/QxvqKVCaitRVBLgnp4I5iEW5YAMhQuir5IdnQ6BDkk433f1qzVztSYN+W1RYS7Ntu+PKlJt6g0ME0LpbO2OjdBoIppcxqbyP6T2hBlq1pSwFBEEoN7l9crsB+UKo88sH7MNwxGMkwpg/aDodUcBCl1d8itDTKSsEWIYRSytVicRHi9UPJ8zg2Gnz4iftdJwKaUnv9Hs1qQ/vBFwnmFZ506CXo1FIKnc1xzVLW9nQqk2ICLyrUEvx8bVmDMXiYP6IaK92jsNt1rVCvDDXdRYl7Gtb266+Ks9JsG31sZKxlxnxZ6Ac8Jcm4ZKt947/7mQIDAQAB";
        config.alipayPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAsHLwfJ7dM+4llsUb9XzK40/a10+u8mCufyTinviJp3L5TCiOjdPSxz2K5Gy2fyTFuAUJz6r2HyMc5TP6X4OvRf/MLKUDZgaN6f+KmPmg4Rp2vt7xAxmsaNYYxmH/3Qag3LjcRXrh3du2l8eGmw0W0L+WK7xpQ3IFzZ4EAUSwNmmaJcESxp3g9CJ2XiDABZVyqSKK6V75BGENIgXeFbilV+anCoaYrHAbk4R2mRdq+4HLq78+9vi1X92WOR+j1V2WabIqmsoU8p5IiIfjdjo+AzLhP9Kq4T6jzJlq340rbx8/CambiHcdD0MzN04jeOJIT3IZTtmfeuwsU5bjXkwzmQIDAQAB";
        return config;
    }

}
