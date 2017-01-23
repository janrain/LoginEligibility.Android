# LoginEligibility.Android
Android Library that works with the MIAA Policy Checker service

**Usage**

The example code below was derived from implementing the framework with the Janrain SimpleCaptureDemo application found here:
https://github.com/janrain/jump.android/tree/master/Samples/SimpleDemo

Add the library to your project by importing it as a Gradle project module.  Call it something obvious like 'logineligibility':

	dependencies {
	    compile 'com.google.code.gson:gson:2.2.4'
	    compile project(':jump')
	    compile project(':logineligibility')
	}

In your applications MainActivity (or equivalent) import the LEService and LEServiceConfiguration:

	import com.janrain.android.logineligibility.LEService;
	import com.janrain.android.logineligibility.LEServiceConfiguration;

Where ever you will be retrieving the Capture Access Token make sure the class implements the LEService.LoginEligibilityResultHandler:

	private class MySignInResultHandler implements Jump.SignInResultHandler, Jump.SignInCodeHandler, LEService.LoginEligibilityResultHandler {
		...
	}

In this example we will hook into the Janrain Jump.SignInResultHandler's "onSuccess" method, retrieve the access token, and then pass it to the LoginEligibility Service:

	public void onSuccess() {
        LogUtils.logd("Access Token: " + Jump.getAccessToken());

        LogUtils.logd("User Logged in: " + String.valueOf(Jump.getSignedInUser()));
        LEServiceConfiguration leConfig = new LEServiceConfiguration();
        leConfig.captureApplicationId = Jump.getCaptureAppId();
        leConfig.captureClientId = Jump.getCaptureClientId();
        leConfig.captureFlowName = Jump.getCaptureFlowName();
        leConfig.captureFlowLocale = Jump.getCaptureLocale();
        leConfig.captureFlowVersion = Jump.getCaptureFlowVersion();
        leConfig.policyCheckerStage = "dev";
        leConfig.policyCheckerTenant = "tccc_shared_tenant";
        leConfig.policyCheckerHost = "https://something.execute-api.somewhere.amazonaws.com";

        LEService leService = new LEService();
        leService.init(leConfig);

        leService.checkLoginWithToken(Jump.getAccessToken(), this);
    }

In the same handler class implement the LEService.LoginEligibilityResultHandler's "onSuccess and "onFailure" methods.  Update the internal method logic as needed:

	@Override
    public void onLEServiceSuccess(JSONObject json) {
        LogUtils.logd("LE Service Success: " + json.toString());
        try {
            if (json.getString("outcome").equals("Complies")) {
                AlertDialog.Builder b = new AlertDialog.Builder(MainActivity.this);
                b.setMessage("User Complies.");
                b.setNeutralButton("Dismiss", null);
                AlertDialog alertDialog = b.create();
                alertDialog.show();
            }else{
                Jump.signOutCaptureUser(MainActivity.this);
                AlertDialog.Builder b = new AlertDialog.Builder(MainActivity.this);
                b.setMessage("User Violates.");
                b.setNeutralButton("Dismiss", null);
                AlertDialog alertDialog = b.create();
                alertDialog.show();
            }
        }catch(JSONException jex){
            LogUtils.logd("LE Service JSON Error: " + json.toString());
            Jump.signOutCaptureUser(MainActivity.this);
            AlertDialog.Builder b = new AlertDialog.Builder(MainActivity.this);
            b.setMessage("Policy Verification Error.");
            b.setNeutralButton("Dismiss", null);
            AlertDialog alertDialog = b.create();
            alertDialog.show();
        }

    }

    @Override
    public void onLEServiceFailure(JSONObject json) {
        LogUtils.logd("LE Service Error: " + json.toString());
        Jump.signOutCaptureUser(MainActivity.this);
        AlertDialog.Builder b = new AlertDialog.Builder(MainActivity.this);
        b.setMessage("Sign-in failed.");
        b.setNeutralButton("Dismiss", null);
        AlertDialog alertDialog = b.create();
        alertDialog.show();
    }
