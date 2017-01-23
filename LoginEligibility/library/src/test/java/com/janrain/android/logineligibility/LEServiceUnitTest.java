package com.janrain.android.logineligibility;

import android.webkit.URLUtil;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertNotEquals;

/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 Copyright (c) 2017, Janrain, Inc.

 All rights reserved.

 Redistribution and use in source and binary forms, with or without modification,
 are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright notice, this
 list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice,
 this list of conditions and the following disclaimer in the documentation and/or
 other materials provided with the distribution.
 * Neither the name of the Janrain, Inc. nor the names of its
 contributors may be used to endorse or promote products derived from this
 software without specific prior written permission.


 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

@RunWith(PowerMockRunner.class)
@PrepareForTest({URLUtil.class})
public class LEServiceUnitTest {

    LEServiceConfiguration goodConfig;
    LEServiceConfiguration missingValConfig;
    LEServiceConfiguration badUrlConfig;


    @Before
    public void setUp() throws Exception {
        goodConfig = new LEServiceConfiguration();
        goodConfig.captureApplicationId = "somecaptureappid";
        goodConfig.captureClientId = "somecaptureclientid";
        goodConfig.captureFlowName = "someflowname";
        goodConfig.captureFlowLocale = "some-locale";
        goodConfig.captureFlowVersion = "some-version";
        goodConfig.policyCheckerStage = "dev";
        goodConfig.policyCheckerTenant = "some-tenant";
        goodConfig.policyCheckerHost = "http://api.somevalidurl.com";

        missingValConfig = (LEServiceConfiguration) cloneObject(goodConfig);
        missingValConfig.captureApplicationId = null;

        badUrlConfig = (LEServiceConfiguration) cloneObject(goodConfig);
        badUrlConfig.policyCheckerHost = "justsomestring";

    }

    @Test
    public void configHasRequiredFields_True() throws Exception {
        PowerMockito.mockStatic(URLUtil.class);
        PowerMockito.when(URLUtil.isValidUrl("http://api.somevalidurl.com")).thenReturn(true);
        assertTrue(LEService.configHasRequiredFields(goodConfig));
    }

    @Test(expected = LEServiceException.class)
    public void configHasRequiredFields_MissingField() throws Exception {

        PowerMockito.mockStatic(URLUtil.class);
        PowerMockito.when(URLUtil.isValidUrl("http://api.somevalidurl.com")).thenReturn(true);
        assertFalse(LEService.configHasRequiredFields(missingValConfig));
    }

    @Test(expected = LEServiceException.class)
    public void configHasRequiredFields_NotURL() throws Exception {

        PowerMockito.mockStatic(URLUtil.class);
        PowerMockito.when(URLUtil.isValidUrl("justsomestring")).thenReturn(false);
        assertFalse(LEService.configHasRequiredFields(badUrlConfig));
    }

    @Test
    public void testinit_True() throws Exception {

        PowerMockito.mockStatic(URLUtil.class);
        PowerMockito.when(URLUtil.isValidUrl("http://api.somevalidurl.com")).thenReturn(true);
        LEService leService = new LEService();
        leService.init(goodConfig);
        assertEquals(goodConfig, leService.state.leServiceConfiguration);
    }

    @Test(expected = LEServiceException.class)
    public void init_Fails() throws Exception {

        PowerMockito.mockStatic(URLUtil.class);
        PowerMockito.when(URLUtil.isValidUrl("http://api.somevalidurl.com")).thenReturn(true);
        LEService leService = new LEService();
        leService.init(missingValConfig);
        assertNotEquals(missingValConfig, leService.state.leServiceConfiguration);
    }



    private static Object cloneObject(Object obj){
        try{
            Object clone = obj.getClass().newInstance();
            for (Field field : obj.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                if(field.get(obj) == null || Modifier.isFinal(field.getModifiers())){
                    continue;
                }
                if(field.getType().isPrimitive() || field.getType().equals(String.class)
                        || field.getType().getSuperclass().equals(Number.class)
                        || field.getType().equals(Boolean.class)){
                    field.set(clone, field.get(obj));
                }else{
                    Object childObj = field.get(obj);
                    if(childObj == obj){
                        field.set(clone, clone);
                    }else{
                        field.set(clone, cloneObject(field.get(obj)));
                    }
                }
            }
            return clone;
        }catch(Exception e){
            return null;
        }
    }
}
