#import "FirebaseAuthenticationPlugin.h"

@implementation FirebaseAuthenticationPlugin

static FIRUser* anonymousUser;

- (void)pluginInitialize {
    NSLog(@"Starting Firebase Authentication plugin");

    if(![FIRApp defaultApp]) {
        [FIRApp configure];
    }
}

- (void)getIdToken:(CDVInvokedUrlCommand *)command {
    BOOL forceRefresh = [[command.arguments objectAtIndex:0] boolValue];
    FIRUser *user = [FIRAuth auth].currentUser;

    if (user) {
        [user getIDTokenForcingRefresh:forceRefresh completion:^(NSString *token, NSError *error) {
            CDVPluginResult *pluginResult;
            if (error) {
                pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:error.localizedDescription];
            } else {
                pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:token];
            }

            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        }];
    } else {
        CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"User must be signed in"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }
}

- (void)createUserWithEmailAndPassword:(CDVInvokedUrlCommand *)command {
    NSString* email = [command.arguments objectAtIndex:0];
    NSString* password = [command.arguments objectAtIndex:1];

    [[FIRAuth auth] createUserWithEmail:email
                               password:password
                             completion:^(FIRAuthDataResult *result, NSError *error) {
        [self.commandDelegate sendPluginResult:[self createAuthResult:result
                                                            withError:error] callbackId:command.callbackId];
    }];
}

- (void)sendEmailVerification:(CDVInvokedUrlCommand *)command {
    FIRUser *currentUser = [FIRAuth auth].currentUser;

    if (currentUser) {
        [currentUser sendEmailVerificationWithCompletion:^(NSError *_Nullable error) {
            CDVPluginResult *pluginResult;
            if (error) {
                pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:error.localizedDescription];
            } else {
                pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
            }

            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        }];
    } else {
        CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"User must be signed in"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }
}

- (void)sendPasswordResetEmail:(CDVInvokedUrlCommand *)command {
    NSString* email = [command.arguments objectAtIndex:0];

    [[FIRAuth auth] sendPasswordResetWithEmail:email completion:^(NSError *_Nullable error) {
        CDVPluginResult *pluginResult;
        if (error) {
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:error.localizedDescription];
        } else {
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        }

        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}

- (void)signInWithEmailAndPassword:(CDVInvokedUrlCommand *)command {
    NSString* email = [command.arguments objectAtIndex:0];
    NSString* password = [command.arguments objectAtIndex:1];

    [[FIRAuth auth] signInWithEmail:email
                           password:password
                         completion:^(FIRAuthDataResult *result, NSError *error) {
        [self.commandDelegate sendPluginResult:[self createAuthResult:result
                                                            withError:error] callbackId:command.callbackId];
    }];
}

- (void)signInAnonymously:(CDVInvokedUrlCommand *)command {
    [[FIRAuth auth] signInAnonymouslyWithCompletion:^(FIRAuthDataResult *result, NSError *error) {
        [self.commandDelegate sendPluginResult:[self createAuthResult:result
                                                            withError:error] callbackId:command.callbackId];
    }];
}

- (void)deleteCurrentAnonymousUser:(CDVInvokedUrlCommand *)command {
    [anonymousUser deleteWithCompletion:^(NSError *_Nullable error) {
        CDVPluginResult *pluginResult;
        if (error) {
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:error.localizedDescription];
        } else {
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        }
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}

- (void)signInWithGoogle:(CDVInvokedUrlCommand *)command {
    NSString* idToken = [command.arguments objectAtIndex:0];
    NSString* accessToken = [command.arguments objectAtIndex:1];

    FIRAuthCredential *credential =
        [FIRGoogleAuthProvider credentialWithIDToken:idToken
                                         accessToken:accessToken];

    [[FIRAuth auth] signInAndRetrieveDataWithCredential:credential
                                             completion:^(FIRAuthDataResult *result, NSError *error) {
        [self.commandDelegate sendPluginResult:[self createAuthResult:result
                                                            withError:error] callbackId:command.callbackId];
    }];
}

- (void)signInWithFacebook:(CDVInvokedUrlCommand *)command {
    NSString* accessToken = [command.arguments objectAtIndex:0];

    FIRAuthCredential *credential =
        [FIRFacebookAuthProvider credentialWithAccessToken:accessToken];

    [[FIRAuth auth] signInAndRetrieveDataWithCredential:credential
                                             completion:^(FIRAuthDataResult *result, NSError *error) {
        [self.commandDelegate sendPluginResult:[self createAuthResult:result
                                                            withError:error] callbackId:command.callbackId];
    }];
}

- (void)signInWithTwitter:(CDVInvokedUrlCommand *)command {
    NSString* token = [command.arguments objectAtIndex:0];
    NSString* secret = [command.arguments objectAtIndex:1];

    FIRAuthCredential *credential =
        [FIRTwitterAuthProvider credentialWithToken:token
                                             secret:secret];

    [[FIRAuth auth] signInAndRetrieveDataWithCredential:credential
                                             completion:^(FIRAuthDataResult *result, NSError *error) {
        [self.commandDelegate sendPluginResult:[self createAuthResult:result
                                                            withError:error] callbackId:command.callbackId];
    }];
}

- (void)signInWithVerificationId:(CDVInvokedUrlCommand*)command {
    NSString* verificationId = [command.arguments objectAtIndex:0];
    NSString* smsCode = [command.arguments objectAtIndex:1];

    FIRAuthCredential *credential = [[FIRPhoneAuthProvider provider]
            credentialWithVerificationID:verificationId
                        verificationCode:smsCode];

    [[FIRAuth auth] signInAndRetrieveDataWithCredential:credential
                                             completion:^(FIRAuthDataResult *result, NSError *error) {
        [self.commandDelegate sendPluginResult:[self createAuthResult:result
                                                            withError:error] callbackId:command.callbackId];
    }];
}

- (void)verifyPhoneNumber:(CDVInvokedUrlCommand*)command {
    NSString* phoneNumber = [command.arguments objectAtIndex:0];

    [[FIRPhoneAuthProvider provider] verifyPhoneNumber:phoneNumber
                                            UIDelegate:nil
                                            completion:^(NSString* verificationId, NSError* error) {
        CDVPluginResult *pluginResult;
        if (error) {
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:error.localizedDescription];
        } else {
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:verificationId];
        }

        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}

- (void)signOut:(CDVInvokedUrlCommand*)command {
    NSError *signOutError;
    CDVPluginResult *pluginResult;

    if ([[FIRAuth auth] signOut:&signOutError]) {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    } else {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:signOutError.localizedDescription];
    }

    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)setLanguageCode:(CDVInvokedUrlCommand*)command {
    NSString* languageCode = [command.arguments objectAtIndex:0];
    if (languageCode) {
        [FIRAuth auth].languageCode = languageCode;
    } else {
        [[FIRAuth auth] useAppLanguage];
    }

    CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (CDVPluginResult*) createAuthResult:(FIRAuthDataResult*)result withError:(NSError*)error {
    CDVPluginResult *pluginResult;
    if (error) {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:@{
          @"code": @(error.code),
          @"message": error.localizedDescription
        }];
    } else {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:[self userToDictionary:result.user]];
    }
    return pluginResult;
}

- (void)setAuthStateChanged:(CDVInvokedUrlCommand*)command {
    self.handle = [[FIRAuth auth] addAuthStateDidChangeListener:^(FIRAuth *_Nonnull auth, FIRUser *_Nullable user) {
        dispatch_async(dispatch_get_main_queue(), ^{
            CDVPluginResult *pluginResult;
            if (user) {
                if (user.isAnonymous) {
                    anonymousUser = user;
                }
                pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:[self userToDictionary:user]];
            } else {
                pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
            }
            [pluginResult setKeepCallbackAsBool:YES];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        });
    }];
}

- (void)changePassword:(CDVInvokedUrlCommand *)command {
    NSString* newPassword = [command.arguments objectAtIndex:0];
    FIRUser *user = [FIRAuth auth].currentUser;
    [user updatePassword:newPassword completion:^(NSError *_Nullable error) {
        CDVPluginResult *pluginResult;
        if (error) {
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:[self exceptionToDictionary:error]];
        } else {
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        }
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}

- (void)updateEmail:(CDVInvokedUrlCommand *)command {
    NSString* email = [command.arguments objectAtIndex:0];
    FIRUser *user = [FIRAuth auth].currentUser;
    
    [user updateEmail:email completion:^(NSError *_Nullable error) {
        dispatch_async(dispatch_get_main_queue(), ^{
            CDVPluginResult *pluginResult;
            if (error) {
                pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:[self exceptionToDictionary:error]];
            } else {
                pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
            }
            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        });
    }];
}

- (void)updateProfile:(CDVInvokedUrlCommand *)command {
    NSString* displayName = [command.arguments objectAtIndex:0];
    NSString* photoURL = [command.arguments objectAtIndex:1];
    
    FIRUserProfileChangeRequest *changeRequest = [[FIRAuth auth].currentUser profileChangeRequest];
    if(displayName) {
        changeRequest.displayName = displayName;
    }
    if(photoURL) {
        NSString *charactersToEscape = @"!*'();:@&=+$,/?%#[]";
        NSCharacterSet *allowedCharacters = [[NSCharacterSet characterSetWithCharactersInString:charactersToEscape] invertedSet];
        
        NSString *url = [NSString stringWithFormat:@"%@", photoURL];
        NSString *encodedUrl = [url stringByAddingPercentEncodingWithAllowedCharacters:allowedCharacters];
        changeRequest.photoURL = [NSURL URLWithString: encodedUrl];
    }
    
    [changeRequest commitChangesWithCompletion:^(NSError *_Nullable error) {
//        dispatch_async(dispatch_get_main_queue(), ^{
            CDVPluginResult *pluginResult;
            if (error) {
                pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:[self exceptionToDictionary:error]];
            } else {
                pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
            }
            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
//        });
    }];
}

- (void)currentUser:(CDVInvokedUrlCommand *)command {
    FIRUser *user = [FIRAuth auth].currentUser;
    CDVPluginResult *pluginResult;
    if (user) {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:[self userToDictionary:user]];
    } else {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    }
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)reauthenticateWithCredential:(CDVInvokedUrlCommand *)command {
    FIRUser *user = [FIRAuth auth].currentUser;
    NSString* email = [command.arguments objectAtIndex:0];
    NSString* password = [command.arguments objectAtIndex:1];
    
    FIRAuthCredential *credential = [FIREmailAuthProvider credentialWithEmail: email password: password];
    [user reauthenticateWithCredential:credential completion:^(NSError *_Nullable error) {
        CDVPluginResult *pluginResult;
        if (error) {
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:[self exceptionToDictionary:error]];
        } else {
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        }
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}

- (NSDictionary*)userToDictionary:(FIRUser *)user {
    NSArray<id<FIRUserInfo>> *providerData = user.providerData;
    return @{
        @"uid": user.uid,
        @"providerId": user.providerID,
        @"displayName": user.displayName ? user.displayName : @"",
        @"email": user.email ? user.email : @"",
        @"phoneNumber": user.phoneNumber ? user.phoneNumber : @"",
        @"photoURL": user.photoURL ? user.photoURL.absoluteString : @"",
        @"providerData": (providerData == nil || [providerData count] == 0) ? @[] : @[providerData[0].providerID],
        @"isAnonymous": user.isAnonymous ? @YES : @NO
    };
}

- (NSDictionary*)exceptionToDictionary:(NSError *)error {
    return @{
         @"code": @(error.code),
         @"message": error.localizedDescription
     };
}

@end
