package by.chemerisuk.cordova.firebase;

import android.util.Log;
import android.net.Uri;

import by.chemerisuk.cordova.support.CordovaMethod;
import by.chemerisuk.cordova.support.ReflectiveCordovaPlugin;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.FirebaseAuth.AuthStateListener;
import com.google.firebase.auth.GetTokenResult;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.auth.TwitterAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuthException;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CordovaInterface;

import static java.util.concurrent.TimeUnit.MILLISECONDS;


public class FirebaseAuthenticationPlugin extends ReflectiveCordovaPlugin implements OnCompleteListener, AuthStateListener {
    private static final String TAG = "FirebaseAuthentication";

    private FirebaseAuth firebaseAuth;
    private PhoneAuthProvider phoneAuthProvider;
    private CallbackContext signinCallback;
    private CallbackContext authStateCallback;
    private FirebaseUser anonymousUser;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        Log.d(TAG, "Starting Firebase Authentication plugin");

        this.firebaseAuth = FirebaseAuth.getInstance();
        this.phoneAuthProvider = PhoneAuthProvider.getInstance();
    }

    @CordovaMethod
    private void getIdToken(boolean forceRefresh, final CallbackContext callbackContext) {
        FirebaseUser user = firebaseAuth.getCurrentUser();

        if (user == null) {
            callbackContext.error("User is not authorized");
        } else {
            user.getIdToken(forceRefresh)
                .addOnCompleteListener(cordova.getActivity(), new OnCompleteListener<GetTokenResult>() {
                    @Override
                    public void onComplete(Task<GetTokenResult> task) {
                        if (task.isSuccessful()) {
                            callbackContext.success(task.getResult().getToken());
                        } else {
                            callbackContext.error(task.getException().getMessage());
                        }
                    }
                });
        }
    }

    @CordovaMethod
    private void createUserWithEmailAndPassword(String email, String password, CallbackContext callbackContext) {
        this.signinCallback = callbackContext;

        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(cordova.getActivity(), this);
    }

    @CordovaMethod
    private void sendEmailVerification(final CallbackContext callbackContext) {
        FirebaseUser user = firebaseAuth.getCurrentUser();

        if (user == null) {
            callbackContext.error("User is not authorized");
        } else {
            user.sendEmailVerification()
                .addOnCompleteListener(cordova.getActivity(), new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(Task<Void> task) {
                        if (task.isSuccessful()) {
                            callbackContext.success();
                        } else {
                            callbackContext.error(task.getException().getMessage());
                        }
                    }
                });
        }
    }

    @CordovaMethod
    private void sendPasswordResetEmail(String email, final CallbackContext callbackContext) {
        firebaseAuth.sendPasswordResetEmail(email)
            .addOnCompleteListener(cordova.getActivity(), new OnCompleteListener<Void>() {
                @Override
                public void onComplete(Task<Void> task) {
                    if (task.isSuccessful()) {
                        callbackContext.success();
                    } else {
                        callbackContext.error(task.getException().getMessage());
                    }
                }
            });
    }

    @CordovaMethod
    private void signInAnonymously(CallbackContext callbackContext) {
        this.signinCallback = callbackContext;
        OnCompleteListener plugin = this;
        firebaseAuth.signInAnonymously()
            .addOnCompleteListener(cordova.getActivity(), new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(Task<AuthResult> task) {
                    anonymousUser = task.getResult().getUser();
                    plugin.onComplete(task);
                }
            });
    }

    @CordovaMethod
    private void signInWithEmailAndPassword(String email, String password, CallbackContext callbackContext) {
        this.signinCallback = callbackContext;

        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(cordova.getActivity(), this);
    }

    @CordovaMethod
    private void signInWithGoogle(String idToken, String accessToken, CallbackContext callbackContext) {
        signInWithCredential(GoogleAuthProvider.getCredential(idToken, accessToken), callbackContext);
    }

    @CordovaMethod
    private void signInWithFacebook(String accessToken, CallbackContext callbackContext) {
        signInWithCredential(FacebookAuthProvider.getCredential(accessToken), callbackContext);
    }

    @CordovaMethod
    private void signInWithTwitter(String token, String secret, CallbackContext callbackContext) {
        signInWithCredential(TwitterAuthProvider.getCredential(token, secret), callbackContext);
    }

    private void signInWithCredential(final AuthCredential credential, CallbackContext callbackContext) {
        this.signinCallback = callbackContext;

        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(cordova.getActivity(), this);
    }

    @CordovaMethod
    private void signInWithVerificationId(String verificationId, String code, CallbackContext callbackContext) {
        this.signinCallback = callbackContext;

        signInWithPhoneCredential(PhoneAuthProvider.getCredential(verificationId, code));
    }

    @CordovaMethod
    private void verifyPhoneNumber(String phoneNumber, long timeoutMillis, final CallbackContext callbackContext) {
        phoneAuthProvider.verifyPhoneNumber(phoneNumber, timeoutMillis, MILLISECONDS, cordova.getActivity(),
            new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                @Override
                public void onVerificationCompleted(PhoneAuthCredential credential) {
                    signInWithPhoneCredential(credential);
                }

                @Override
                public void onCodeSent(String verificationId, PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                    callbackContext.success(verificationId);
                }

                @Override
                public void onVerificationFailed(FirebaseException e) {
                    callbackContext.error(e.getMessage());
                }
            }
        );
    }

    private void signInWithPhoneCredential(PhoneAuthCredential credential) {
        FirebaseUser user = firebaseAuth.getCurrentUser();

        if (user == null) {
            firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(cordova.getActivity(), FirebaseAuthenticationPlugin.this);
        } else {
            user.updatePhoneNumber(credential)
                .addOnCompleteListener(cordova.getActivity(), FirebaseAuthenticationPlugin.this);
        }
    }

    @CordovaMethod
    private void signOut(CallbackContext callbackContext) {
        firebaseAuth.signOut();

        callbackContext.success();
    }

    @CordovaMethod
    private void setLanguageCode(String languageCode, CallbackContext callbackContext) {
        if (languageCode == null) {
            firebaseAuth.useAppLanguage();
        } else {
            firebaseAuth.setLanguageCode(languageCode);
        }

        callbackContext.success();
    }

    @CordovaMethod
    private void setAuthStateChanged(boolean disable, CallbackContext callbackContext) {
        this.authStateCallback = disable ? null : callbackContext;

        if (disable) {
            firebaseAuth.removeAuthStateListener(this);
        } else {
            firebaseAuth.addAuthStateListener(this);
        }
    }

    @Override
    public void onComplete(Task task) {
        if (this.signinCallback != null) {
            if (task.isSuccessful()) {
                this.signinCallback.success(getProfileData(firebaseAuth.getCurrentUser()));
            } else {
                this.signinCallback.error(getTaskExceptionData(task));
            }

            this.signinCallback = null;
        }
    }

    @Override
    public void onAuthStateChanged(FirebaseAuth auth) {
        if (this.authStateCallback != null) {
            PluginResult pluginResult;
            FirebaseUser user = firebaseAuth.getCurrentUser();

            if (user != null) {
                pluginResult = new PluginResult(PluginResult.Status.OK, getProfileData(user));
            } else {
                pluginResult = new PluginResult(PluginResult.Status.OK, false);
            }

            pluginResult.setKeepCallback(true);
            this.authStateCallback.sendPluginResult(pluginResult);
        }
    }

    private static JSONObject getTaskExceptionData(Task task) {
        Exception exception = task.getException();
        JSONObject result = new JSONObject();
        try {
            result.put("message", exception.getMessage());
            if (exception instanceof FirebaseAuthException) {
                FirebaseAuthException firebaseAuthException = (FirebaseAuthException)task.getException();
                result.put("code", firebaseAuthException.getErrorCode());
            }
        } catch (JSONException e) {
            Log.e(TAG, "Unhandler error in getTaskExceptionData", e);
        }
        return result;
    }

    private static JSONObject getProfileData(FirebaseUser user) {
        JSONObject result = new JSONObject();

        try {
            // Fixes https://stackoverflow.com/questions/42881700/firebaseuser-isanonymous-always-returns-false-after-profile-update-firebase-1
            Boolean isAnonymous = user.isAnonymous() || user.getProviders().size() == 0;
            result.put("uid", user.getUid());
            result.put("displayName", user.getDisplayName());
            result.put("email", user.getEmail());
            result.put("phoneNumber", user.getPhoneNumber());
            result.put("photoURL", user.getPhotoUrl());
            result.put("providerId", user.getProviderId());
            result.put("providerData", new JSONArray(user.getProviders()));
            result.put("isAnonymous", isAnonymous);
        } catch (JSONException e) {
            Log.e(TAG, "Fail to process getProfileData", e);
        }

        return result;
    }

    @CordovaMethod
    private void updateEmail(String email, CallbackContext callbackContext) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        user.updateEmail(email).addOnCompleteListener(cordova.getActivity(), new OnCompleteListener<Void>() {
            @Override
            public void onComplete(Task<Void> task) {
                if (task.isSuccessful()) {
                    callbackContext.success();
                } else {
                    callbackContext.error(getTaskExceptionData(task));
                }
            }
        });
    }

    @CordovaMethod
    private void changePassword(String password, CallbackContext callbackContext) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        user.updatePassword(password).addOnCompleteListener(cordova.getActivity(), new OnCompleteListener<Void>() {
            @Override
            public void onComplete(Task<Void> task) {
                if (task.isSuccessful()) {
                    callbackContext.success();
                } else {
                    callbackContext.error(getTaskExceptionData(task));
                }
            }
        });
    }

    @CordovaMethod
    private void updateProfile(String displayName, String photoURL, CallbackContext callbackContext) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        UserProfileChangeRequest.Builder profileUpdates = new UserProfileChangeRequest.Builder();
        if (displayName != null) {
            profileUpdates.setDisplayName(displayName);
        }
        if (photoURL != null) {
            Uri photoUri = Uri.parse(photoURL);
            profileUpdates.setPhotoUri(photoUri);
        }
        user.updateProfile(profileUpdates.build())
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(Task<Void> task) {
                        if (task.isSuccessful()) {
                            callbackContext.success();
                        } else {
                            callbackContext.error(task.getException().getMessage());
                        }
                    }
                });
    }

    @CordovaMethod
    private void currentUser(CallbackContext callbackContext) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            callbackContext.success(getProfileData(user));
        } else {
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, false));
        }
    }

    @CordovaMethod
    private void deleteCurrentAnonymousUser(CallbackContext callbackContext) {
        // Not implemented
        // Firebase Android SDK doesn't seem to allow to delete user that is no longer signed in
        callbackContext.success();
        /*
        anonymousUser.delete().addOnCompleteListener(cordova.getActivity(), new OnCompleteListener<Void>() {
            @Override
            public void onComplete(Task<Void> task) {
                if (task.isSuccessful()) {
                    callbackContext.success();
                } else {
                    callbackContext.error(task.getException().getMessage());
                }
            }
        });
        */
    }

    @CordovaMethod
    private void reauthenticateWithCredential(String email, String password, CallbackContext callbackContext) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        AuthCredential credential = EmailAuthProvider.getCredential(email, password);
        user.reauthenticate(credential)
            .addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(Task<Void> task) {
                    if (task.isSuccessful()) {
                        callbackContext.success();
                    } else {
                        callbackContext.error(task.getException().getMessage());
                    }
                }
            });
    }
}
