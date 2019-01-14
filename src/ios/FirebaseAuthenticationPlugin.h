#import <Cordova/CDV.h>
#import "Firebase.h"
@import FirebaseAuth;

@interface FirebaseAuthenticationPlugin : CDVPlugin

- (void)getIdToken:(CDVInvokedUrlCommand*)command;
- (void)createUserWithEmailAndPassword:(CDVInvokedUrlCommand*)command;
- (void)sendEmailVerification:(CDVInvokedUrlCommand*)command;
- (void)sendPasswordResetEmail:(CDVInvokedUrlCommand*)command;
- (void)signInWithEmailAndPassword:(CDVInvokedUrlCommand*)command;
- (void)signInAnonymously:(CDVInvokedUrlCommand*)command;
- (void)signInWithGoogle:(CDVInvokedUrlCommand*)command;
- (void)signInWithFacebook:(CDVInvokedUrlCommand*)command;
- (void)signInWithTwitter:(CDVInvokedUrlCommand*)command;
- (void)signInWithVerificationId:(CDVInvokedUrlCommand*)command;
- (void)verifyPhoneNumber:(CDVInvokedUrlCommand*)command;
- (void)signOut:(CDVInvokedUrlCommand*)command;
- (void)setLanguageCode:(CDVInvokedUrlCommand*)command;
- (void)setAuthStateChanged:(CDVInvokedUrlCommand*)command;
- (void)changePassword:(CDVInvokedUrlCommand*)command;
- (void)updateEmail:(CDVInvokedUrlCommand*)command;
- (void)updateProfile:(CDVInvokedUrlCommand*)command;
- (void)deleteCurrentAnonymousUser:(CDVInvokedUrlCommand*)command;
- (void)currentUser:(CDVInvokedUrlCommand*)command;

@property(strong, nonatomic) FIRAuthStateDidChangeListenerHandle handle;

@end
