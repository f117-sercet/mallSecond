package com.dsc.front.controller;

import com.dsc.common.pojo.Result;
import com.dsc.common.utils.ResultUtil;
import com.dsc.mall.manager.pojo.TbAddress;
import com.dsc.sso.service.AddressService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author  dsc
 *
 */
@RestController
@Api(description = "收货地址")
public class AddressController {

    @Autowired
    private AddressService addressService;

    @RequestMapping(value = "/member/addressList",method = RequestMethod.POST)
    @ApiOperation(value = "获得收货地址")
    public Result<List<TbAddress>> addressList(@RequestBody TbAddress tbAddress){
        List<TbAddress> list=addressService.getAddressList(tbAddress.getUserId());
        return new ResultUtil<List<TbAddress>>().setData(list);
    }
    @RequestMapping(value = "/member/address",method = RequestMethod.POST)
    @ApiOperation(value = "通过id获得收货地址")
    public Result<TbAddress> address(@RequestBody TbAddress tbAddress){

        TbAddress address=addressService.getAddress(tbAddress.getAddressId());
        return new ResultUtil<TbAddress>().setData(address);
    }
}
