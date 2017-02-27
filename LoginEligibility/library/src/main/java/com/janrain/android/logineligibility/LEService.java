package com.janrain.android.logineligibility;

import android.support.annotation.VisibleForTesting;
import android.util.Log;
import android.webkit.URLUtil;
import org.json.JSONException;
import org.json.JSONObject;

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

public class LEService {

    @VisibleForTesting
    static final String LOG_TAG;

    static {
        LOG_TAG = "LEService";
    }

    /*package*/ enum State {
        STATE;

        // Configured values:
        /*package*/ LEServiceConfiguration leServiceConfiguration;

        public boolean initCalled;
    }

    /*package*/ static final State state = State.STATE;

     //public void init(Context context, LEServiceConfiguration leServiceConfig){
    public void init(LEServiceConfiguration leServiceConfig){
        if(configHasRequiredFields(leServiceConfig)){
            state.leServiceConfiguration = leServiceConfig;
        }
    }


    public static boolean configHasRequiredFields(LEServiceConfiguration leServiceConfiguration) throws LEServiceException {
        boolean result = true;

        try {
            if (leServiceConfiguration.captureFlowLocale.isEmpty()) result = false;
            if (leServiceConfiguration.captureClientId.isEmpty()) result = false;
            if (leServiceConfiguration.captureApplicationId.isEmpty()) result = false;
            if (leServiceConfiguration.captureFlowVersion.isEmpty()) result = false;
            if (leServiceConfiguration.captureFlowName.isEmpty()) result = false;
            if (leServiceConfiguration.policyCheckerStage.isEmpty()) result = false;
            if (leServiceConfiguration.policyCheckerTenant.isEmpty()) result = false;
            if (!URLUtil.isValidUrl(leServiceConfiguration.policyCheckerHost)) result = false;
        }catch (Exception ex){
            result = false;
        }
        if(!result) throw new LEServiceException("Invalid LEServiceConfiguration Detected");
        return result;

    }

    private void checkLogin(JSONObject subjectKey, final LoginEligibilityResultHandler handler){

        PolicyCheckerTaskParams taskParams = new PolicyCheckerTaskParams(state.leServiceConfiguration, subjectKey);

        PolicyCheckerTask task = new PolicyCheckerTask();
        task.init(taskParams, new PolicyCheckerTask.PolicyCheckerTaskCompleted(){
            @Override
            public void onPolicyCheckerTaskCompleted(JSONObject response){
                if (response != null) {
                    if (response.has("errorCode")){
                        handler.onLEServiceFailure(response);
                    }else{
                        handler.onLEServiceSuccess(response);
                    }
                }else{
                    try{
                        JSONObject errObject = new JSONObject();
                        errObject.put("errorCode", "Null or Empty Response from Policy Checker Server");
                        handler.onLEServiceFailure(errObject);
                    } catch(JSONException ex){
                        Log.e(LOG_TAG, "Error Creating checkLogin Error JSON");
                        throw new LEServiceException("Error Creating checkLogin Error JSON");
                    }
                }
            }
        });

        task.execute();

    }

    public void checkLoginWithToken(String accessToken, LoginEligibilityResultHandler handler){
        if(accessToken.length() > 0){
            try {
                JSONObject jsonObj = new JSONObject();
                jsonObj.put("accessToken", accessToken);
                checkLogin(jsonObj,handler);

            }catch (JSONException jex){
                Log.e(LOG_TAG, "Error Creating AccessToken JSON");
                throw new LEServiceException("Error Creating AccessToken JSON");
            }


        }else{
            Log.e(LOG_TAG, "Empty AccessToken submitted");
            throw new LEServiceException("Empty AccessToken submitted");
        }
    }

    public void checkLoginWithUUID(String uuid, LoginEligibilityResultHandler handler){
        if(uuid.length() > 0){
            try {
                JSONObject jsonObj = new JSONObject();
                jsonObj.put("id", uuid);
                checkLogin(jsonObj,handler);

            }catch (JSONException jex){
                Log.e(LOG_TAG, "Error Creating UUID JSON");
                throw new LEServiceException("Error Creating UUID JSON");
            }


        }else{
            Log.e(LOG_TAG, "Empty UUID submitted");
            throw new LEServiceException("Empty UUID submitted");
        }
    }


    public interface LoginEligibilityResultHandler {
        void onLEServiceSuccess(JSONObject json);

        void onLEServiceFailure(JSONObject json);
    }


}


