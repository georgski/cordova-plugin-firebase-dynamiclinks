#import <Cordova/CDV.h>
#import "Firebase.h"
@import FirebaseDatabase;

@interface FirebaseDatabasePlugin : CDVPlugin

- (void)on:(CDVInvokedUrlCommand *)command;
- (void)off:(CDVInvokedUrlCommand *)command;
- (void)push:(CDVInvokedUrlCommand *)command;
- (void)set:(CDVInvokedUrlCommand *)command;
- (void)update:(CDVInvokedUrlCommand *)command;
- (void)setOnline:(CDVInvokedUrlCommand *)command;

@property(strong) NSMutableDictionary *listeners;

@end
