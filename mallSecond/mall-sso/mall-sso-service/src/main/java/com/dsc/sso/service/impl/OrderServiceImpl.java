package com.dsc.sso.service.impl;

import com.dsc.common.execetion.MallException;
import com.dsc.common.jedis.JedisClient;
import com.dsc.common.utils.IDUtil;
import com.dsc.mall.manager.dto.DtoUtil;
import com.dsc.mall.manager.dto.front.CartProduct;
import com.dsc.mall.manager.dto.front.Order;
import com.dsc.mall.manager.dto.front.OrderInfo;
import com.dsc.mall.manager.dto.front.PageOrder;
import com.dsc.mall.manager.mapper.*;
import com.dsc.mall.manager.pojo.*;
import com.dsc.sso.service.OrderService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.web.context.support.GenericWebApplicationContext;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * 订单Service实现类
 * @author 60221
 */
public class OrderServiceImpl implements OrderService {

    private final static Logger log= LoggerFactory.getLogger(OrderServiceImpl.class);

    @Autowired
    /**
     * 用户
     */
    private TbMemberMapper tbMemberMapper;
    @Autowired
    /**
     * 订单
     */
    private TbOrderMapper tbOrderMapper;
    @Autowired
    /**
     * 订单商品
     */
    private TbOrderItemMapper tbOrderItemMapper;
    /**
     * 订单物流
     */
    @Autowired
    private TbOrderShippingMapper tbOrderShippingMapper;

    @Autowired
    private TbThanksMapper tbThanksMapper;
    @Autowired
    private JedisClient jedisClient;

    @Value("1209600")
    private int PAY_EXPIRE;
    @Value("${CART_PRE}")
    private String CART_PRE;
    /**
     *
     * @param userId
     * @param page
     * @param size
     * @return
     */
    @Override
    public PageOrder getOrderList(Long userId, int page, int size) {
        //分页
        if(page<=0) {
            page = 1;
        }
        PageHelper.startPage(page,size);

        PageOrder pageOrder=new PageOrder();
        List<Order> list=new ArrayList<>();

        TbOrderExample example=new TbOrderExample();
        TbOrderExample.Criteria criteria= example.createCriteria();
        criteria.andUserIdEqualTo(userId);
        example.setOrderByClause("create_time DESC");
        List<TbOrder> listOrder =tbOrderMapper.selectByExample(example);
        for(TbOrder tbOrder:listOrder){

            judgeOrder(tbOrder);

            Order order=new Order();
            //orderId
            order.setOrderId(Long.valueOf(tbOrder.getOrderId()));
            //orderStatus
            order.setOrderStatus(String.valueOf(tbOrder.getStatus()));
            //createDate
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            String date = formatter.format(tbOrder.getCreateTime());
            order.setCreateDate(date);
            //address
            TbOrderShipping tbOrderShipping=tbOrderShippingMapper.selectByPrimaryKey(tbOrder.getOrderId());
            TbAddress address=new TbAddress();
            address.setUserName(tbOrderShipping.getReceiverName());
            address.setStreetName(tbOrderShipping.getReceiverAddress());
            address.setTel(tbOrderShipping.getReceiverPhone());
            order.setAddressInfo(address);
            //orderTotal
            if(tbOrder.getPayment()==null){
                order.setOrderTotal(new BigDecimal(0));
            }else{
                order.setOrderTotal(tbOrder.getPayment());
            }
            //goodsList
            TbOrderItemExample exampleItem=new TbOrderItemExample();
            TbOrderItemExample.Criteria criteriaItem= exampleItem.createCriteria();
            criteriaItem.andOrderIdEqualTo(tbOrder.getOrderId());
            List<TbOrderItem> listItem =tbOrderItemMapper.selectByExample(exampleItem);
            List<CartProduct> listProduct=new ArrayList<>();
            for(TbOrderItem tbOrderItem:listItem){

                CartProduct cartProduct= DtoUtil.TbOrderItem2CartProduct(tbOrderItem);

                listProduct.add(cartProduct);
            }
            order.setGoodsList(listProduct);
            list.add(order);
        }
        PageInfo<Order> pageInfo=new PageInfo<>(list);
        pageOrder.setTotal(getMemberOrderCount(userId));
        pageOrder.setData(list);
        return pageOrder;
    }


    @Override
    public Order getOrder(Long orderId) {

        Order order=new Order();
        TbOrder tbOrder=tbOrderMapper.selectByPrimaryKey(String.valueOf(orderId));
        if (tbOrder==null){
            throw new MallException("通过id获取订单失败");
        }
        String validTime=judgeOrder(tbOrder);
        if (validTime!=null){
            order.setFinishDate(validTime);
        }
        //orderId
        order.setOrderId(Long.valueOf(tbOrder.getOrderId()));
        //orderStatus
        order.setOrderStatus(String.valueOf(tbOrder.getStatus()));
        //createDate
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        String createDate = formatter.format(tbOrder.getCreateTime());
        //payDate
        if (tbOrder.getPaymentTime()!=null){
            String payDate =formatter.format(tbOrder.getPaymentTime());
            order.setPayDate(payDate);
        }
        //closeDate
        if (tbOrder.getCloseTime()!=null){
            String finishDate = formatter.format(tbOrder.getEndTime());
            order.setFinishDate(finishDate);
        }
        //address
        TbOrderShipping tbOrderShipping=tbOrderShippingMapper.selectByPrimaryKey(tbOrder.getOrderId());
        TbAddress address = new TbAddress();
        address.setUserName(tbOrderShipping.getReceiverName());
        address.setStreetName(tbOrderShipping.getReceiverAddress());
        address.setTel(tbOrderShipping.getReceiverPhone());
        order.setAddressInfo(address);
        //orderTotal
        if (tbOrder.getPayment()==null){
            order.setOrderTotal(new BigDecimal(0));
        }else{
            order.setOrderTotal(tbOrder.getPayment());
        }
        //goodsList

        if(tbOrder.getPayment()==null){
            order.setOrderTotal(new BigDecimal(0));
        }else{
            order.setOrderTotal(tbOrder.getPayment());
        }
        //goodsList
        TbOrderItemExample exampleItem=new TbOrderItemExample();
        TbOrderItemExample.Criteria criteriaItem= exampleItem.createCriteria();
        criteriaItem.andOrderIdEqualTo(tbOrder.getOrderId());
        List<TbOrderItem> listItem =tbOrderItemMapper.selectByExample(exampleItem);
        List<CartProduct> listProduct=new ArrayList<>();
        for(TbOrderItem tbOrderItem:listItem){

            CartProduct cartProduct= DtoUtil.TbOrderItem2CartProduct(tbOrderItem);

            listProduct.add(cartProduct);
        }
        order.setGoodsList(listProduct);
        return order;

    }

    @Override
    public int cancelOrder(Long orderId) {
        TbOrder tbOrder=tbOrderMapper.selectByPrimaryKey(String.valueOf(orderId));
        if (tbOrder==null){
            throw  new MallException("通过id获取订单失败");
        }
        tbOrder.setStatus(5);
        tbOrder.setCloseTime(new Date());
        if (tbOrderMapper.updateByPrimaryKey(tbOrder)!=1){
          throw new MallException("取消订单失败");
        }
        return 1;
    }

    @Override
    public Long createOrder(OrderInfo orderInfo) {
        TbMember member=tbMemberMapper.selectByPrimaryKey(Long.valueOf(orderInfo.getUserId()));

        if (member==null){
            throw new MallException("获取用户下单失败");
        }
        TbOrder order= new TbOrder();
        //生成订单ID
        Long orderId = IDUtil.getRandomId();
        order.setOrderId(String.valueOf(orderId));
        order.setUserId(Long.valueOf(orderInfo.getUserId()));
        order.setPayment(orderInfo.getOrderTotal());
        order.setCreateTime(new Date());
        order.setUpdateTime(new Date());
        //0 未支付 1 已支付 2.未发货 3.已发货 4.交易成功 5.交易关闭 6 交易失败
        order.setStatus(0);

        if (tbOrderMapper.insert(order)!=1){
            throw new MallException("生成订单失败");
        }

        List<CartProduct> list =orderInfo.getGoodsList();
        for (CartProduct cartProduct : list){

            TbOrderItem orderItem = new TbOrderItem();
            //生成订单商品ID
            Long orderItemId = IDUtil.getRandomId();
            orderItem.setId(String.valueOf(orderItemId));
            orderItem.setItemId(String.valueOf(cartProduct.getProductId()));
            orderItem.setOrderId(String.valueOf(orderId));
            orderItem.setNum(Math.toIntExact(cartProduct.getProductNum()));
            orderItem.setPrice(cartProduct.getSalePrice());
            orderItem.setTitle(cartProduct.getProductName());
            orderItem.setPicPath(cartProduct.getProductImg());
            orderItem.setTotalFee(cartProduct.getSalePrice().multiply(BigDecimal.valueOf(cartProduct.getProductNum())));

            if(tbOrderItemMapper.insert(orderItem)!=1){
                throw new MallException("生成订单商品失败");
            }

            //删除购物车中含该订单的商品
            try{
                List<String> jsonList = jedisClient.hvals(CART_PRE + ":" + orderInfo.getUserId());
                for (String json : jsonList) {
                    CartProduct cart = new Gson().fromJson(json,CartProduct.class);
                    if(cart.getProductId().equals(cartProduct.getProductId())){
                        jedisClient.hdel(CART_PRE + ":" + orderInfo.getUserId(),cart.getProductId()+"");
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        //物流表
        TbOrderShipping orderShipping=new TbOrderShipping();
        orderShipping.setOrderId(String.valueOf(orderId));
        orderShipping.setReceiverName(orderInfo.getUserName());
        orderShipping.setReceiverAddress(orderInfo.getStreetName());
        orderShipping.setReceiverPhone(orderInfo.getTel());
        orderShipping.setCreated(new Date());
        orderShipping.setUpdated(new Date());

        if(tbOrderShippingMapper.insert(orderShipping)!=1){
            throw new MallException("生成物流信息失败");
        }

        return orderId;
    }

    @Override
    public int delOrder(Long orderId) {
        if (tbOrderMapper.deleteByPrimaryKey(String.valueOf(orderId))!=1){
            throw new MallException("删除订单失败");
        }
        TbOrderItemExample example=new TbOrderItemExample();
        TbOrderItemExample.Criteria criteria=example.createCriteria();
        criteria.andOrderIdEqualTo(String.valueOf(orderId));
        List<TbOrderItem> list =tbOrderItemMapper.selectByExample(example);
        for (TbOrderItem tbOrderItem:list){
            if (tbOrderMapper.deleteByPrimaryKey(tbOrderItem.getId())!=1){
                throw new MallException("删除订单失败");
            }
        }
        if (tbOrderShippingMapper.deleteByPrimaryKey(String.valueOf(orderId))!=1){
            throw new MallException("删除物流失败");
        }
        return 1;
    }

    @Override
    public int payOrder(TbThanks tbThanks) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String time=sdf.format(new Date());
        tbThanks.setTime(time);
        tbThanks.setDate(new Date());
        TbMember tbMember=tbMemberMapper.selectByPrimaryKey(Long.valueOf(tbThanks.getUserId()));
        if(tbMember!=null){
            tbThanks.setUsername(tbMember.getUsername());
        }
        if(tbThanksMapper.insert(tbThanks)!=1){
            throw new MallException("保存捐赠支付数据失败");
        }

        //设置订单为已付款
        TbOrder tbOrder=tbOrderMapper.selectByPrimaryKey(tbThanks.getOrderId());
        tbOrder.setStatus(1);
        tbOrder.setUpdateTime(new Date());
        tbOrder.setPaymentTime(new Date());
        if(tbOrderMapper.updateByPrimaryKey(tbOrder)!=1){
            throw new MallException("更新订单失败");
        }
        //通知确认
        String tokenName= UUID.randomUUID().toString();
        String token= UUID.randomUUID().toString();
        //设置验证token键值对 tokenName:token
        jedisClient.set(tokenName,token);
        jedisClient.expire(tokenName,PAY_EXPIRE);
        return 1;
    }

    /**
     * 判断订单是否超时未支付
     */
    public String judgeOrder(TbOrder tbOrder){

        String result=null;
        if(tbOrder.getStatus()==0){
            //判断是否已超1天
            long diff=System.currentTimeMillis()-tbOrder.getCreateTime().getTime();
            long days = diff / (1000 * 60 * 60 * 24);
            if(days>=1){
                //设置失效
                tbOrder.setStatus(5);
                tbOrder.setCloseTime(new Date());
                if(tbOrderMapper.updateByPrimaryKey(tbOrder)!=1){
                    throw new MallException( "更新订单失效失败");
                }
            }else {
                //返回到期时间
                long time=tbOrder.getCreateTime().getTime()+1000 * 60 * 60 * 24;
                result= String.valueOf(time);
            }
        }
        return result;
    }

    public int getMemberOrderCount(Long userId){

        TbOrderExample example=new TbOrderExample();
        TbOrderExample.Criteria criteria= example.createCriteria();
        criteria.andUserIdEqualTo(userId);
        List<TbOrder> listOrder =tbOrderMapper.selectByExample(example);
        if(listOrder!=null){
            return listOrder.size();
        }
        return 0;
    }
}

