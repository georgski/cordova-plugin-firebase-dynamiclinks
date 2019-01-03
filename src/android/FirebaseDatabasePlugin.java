package by.chemerisuk.cordova.firebase;

import by.chemerisuk.cordova.support.CordovaMethod;
import by.chemerisuk.cordova.support.ReflectiveCordovaPlugin;
import by.chemerisuk.cordova.support.ReflectiveCordovaPlugin.ExecutionThread;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Logger;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.util.Log;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CordovaInterface;

public class FirebaseDatabasePlugin extends ReflectiveCordovaPlugin {
    private final static String EVENT_TYPE_VALUE = "value";
    private final static String EVENT_TYPE_CHILD_ADDED = "child_added";
    private final static String EVENT_TYPE_CHILD_CHANGED = "child_changed";
    private final static String EVENT_TYPE_CHILD_REMOVED = "child_removed";
    private final static String EVENT_TYPE_CHILD_MOVED = "child_moved";
    private final static Type settableTypeMap = new TypeToken<Map<String, Object>>() {}.getType();
    private final static Type settableTypeList = new TypeToken<List<Object>>() {}.getType();

    private Gson gson;
    private Map<String, Object> listeners;
    private boolean isDestroyed = false;

    private static final String TAG = "FirebaseDatabase";

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        Log.d(TAG, "Starting Firebase Database plugin");
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        this.gson = new Gson();
        this.listeners = new HashMap<String, Object>();
    }

    @Override
    public void onDestroy() {
        this.isDestroyed = true;
    }

    private JSONObject getErrorJsonObject(DatabaseError error) {
        JSONObject data = new JSONObject();
        try {
            data.put("code", error.getCode());
            data.put("message", error.getMessage());
        } catch (JSONException e) {}
        return data;
    }

    private JSONObject getRefJsonObject(String path, String key) {
        JSONObject data = new JSONObject();
        try {
            data.put("key", key);
            data.put("path", path + "/" + key);
        } catch (JSONException e) {}
        return data;
    }

    @CordovaMethod(ExecutionThread.WORKER)
    private void on(String url, String path, String type, JSONObject orderBy, JSONArray includes, JSONObject limit, String uid, CallbackContext callbackContext) throws JSONException {
        final Query query = createQuery(url, path, orderBy, includes, limit);
        final boolean keepCallback = !uid.isEmpty();

        if (EVENT_TYPE_VALUE.equals(type)) {
            ValueEventListener valueListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (isDestroyed) {
                        query.removeEventListener(this);
                    } else {
                        callbackContext.sendPluginResult(createPluginResult(snapshot, keepCallback));
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    if (isDestroyed) {
                        query.removeEventListener(this);
                    } else {
                        callbackContext.error(getErrorJsonObject(databaseError));
                    }
                }
            };

            if (keepCallback) {
                query.addValueEventListener(valueListener);
                listeners.put(uid, valueListener);
            } else {
                query.addListenerForSingleValueEvent(valueListener);
            }
        } else if (keepCallback) {
            final ChildEventListener childListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
                    if (isDestroyed) {
                        query.removeEventListener(this);
                    } else if (EVENT_TYPE_CHILD_ADDED.equals(type)) {
                        callbackContext.sendPluginResult(createPluginResult(snapshot, keepCallback));
                    }
                }

                @Override
                public void onChildChanged(DataSnapshot snapshot, String previousChildName) {
                    if (isDestroyed) {
                        query.removeEventListener(this);
                    } else if (EVENT_TYPE_CHILD_CHANGED.equals(type)) {
                        callbackContext.sendPluginResult(createPluginResult(snapshot, keepCallback));
                    }
                }

                @Override
                public void onChildRemoved(DataSnapshot snapshot) {
                    if (isDestroyed) {
                        query.removeEventListener(this);
                    } else if (EVENT_TYPE_CHILD_REMOVED.equals(type)) {
                        callbackContext.sendPluginResult(createPluginResult(snapshot, keepCallback));
                    }
                }

                @Override
                public void onChildMoved(DataSnapshot snapshot, String previousChildName) {
                    if (isDestroyed) {
                        query.removeEventListener(this);
                    } else if (EVENT_TYPE_CHILD_MOVED.equals(type)) {
                        callbackContext.sendPluginResult(createPluginResult(snapshot, keepCallback));
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    if (isDestroyed) {
                        query.removeEventListener(this);
                    } else {
                        callbackContext.error(getErrorJsonObject(databaseError));
                    }
                }
            };

            query.addChildEventListener(childListener);
            listeners.put(uid, childListener);
        }
    }

    @CordovaMethod(ExecutionThread.WORKER)
    private void off(String url, String path, String uid, CallbackContext callbackContext) {
        Query query = getDb(url).getReference(path);
        Object listener = listeners.get(uid);
        if (listener instanceof ValueEventListener) {
            query.removeEventListener((ValueEventListener)listener);
        } else if (listener instanceof ChildEventListener) {
            query.removeEventListener((ChildEventListener)listener);
        }

        callbackContext.success();
    }

    @CordovaMethod(ExecutionThread.WORKER)
    private void set(String url, String path, Object value, Object priority, CallbackContext callbackContext) {
        DatabaseReference ref = getDb(url).getReference(path);
        DatabaseReference.CompletionListener listener = new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError error, DatabaseReference ref) {
                if (error != null) {
                    callbackContext.error(getErrorJsonObject(error));
                } else {
                    callbackContext.success();
                }
            }
        };

        if (value == null && priority == null) {
            ref.removeValue(listener);
        } else if (priority == null) {
            ref.setValue(toSettable(value), listener);
        } else {
            ref.setValue(toSettable(value), priority, listener);
        }
    }

    @CordovaMethod(ExecutionThread.WORKER)
    private void update(String url, String path, JSONObject value, CallbackContext callbackContext) throws JSONException {
        Map<String, Object> updates = new HashMap<String, Object>();
        for (Iterator<String> it = value.keys(); it.hasNext(); ) {
            String key = it.next();
            updates.put(key, toSettable(value.get(key)));
        }

        DatabaseReference ref = getDb(url).getReference(path);
        ref.updateChildren(updates, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError error, DatabaseReference ref) {
                if (error != null) {
                    callbackContext.error(getErrorJsonObject(error));
                } else {
                    callbackContext.success();
                }
            }
        });
    }

    @CordovaMethod(ExecutionThread.WORKER)
    private void push(String url, String path, Object value, CallbackContext callbackContext) throws JSONException {
        DatabaseReference ref = getDb(url).getReference(path).push();
        String key = ref.getKey();

        if (value == null) {
            callbackContext.success(getRefJsonObject(path, key));
        } else {
            ref.setValue(toSettable(value), new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(DatabaseError error, DatabaseReference ref) {
                    if (error != null) {
                        callbackContext.error(getErrorJsonObject(error));
                    } else {
                        callbackContext.success(getRefJsonObject(path, key));
                    }
                }
            });
        }
    }

    @CordovaMethod(ExecutionThread.WORKER)
    private void setOnline(String url, boolean enabled, CallbackContext callbackContext) {
        if (enabled) {
            getDb(url).goOnline();
        } else {
            getDb(url).goOffline();
        }

        callbackContext.success();
    }

    private Query createQuery(String url, String path, JSONObject orderBy, JSONArray includes, JSONObject limit) throws JSONException {
        Query query = getDb(url).getReference(path);

        if (orderBy != null) {
            if (orderBy.has("key")) {
                query = query.orderByKey();
            } else if (orderBy.has("value")) {
                query = query.orderByValue();
            } else if (orderBy.has("priority")) {
                query = query.orderByPriority();
            } else if (orderBy.has("child")) {
                query = query.orderByChild(orderBy.getString("child"));
            } else {
                throw new JSONException("order is invalid");
            }

            for (int i = 0, n = includes.length(); i < n; ++i) {
                JSONObject filters = includes.getJSONObject(i);

                String key = filters.optString("key", null);
                Object endAt = filters.opt("endAt");
                Object startAt = filters.opt("startAt");
                Object equalTo = filters.opt("equalTo");

                if (startAt != null) {
                    if (startAt instanceof Number) {
                        query = query.startAt((Double)startAt, key);
                    } else if (startAt instanceof Boolean) {
                        query = query.startAt((Boolean)startAt, key);
                    } else {
                        query = query.startAt(startAt.toString(), key);
                    }
                } else if (endAt != null) {
                    if (endAt instanceof Number) {
                        query = query.endAt((Double)endAt, key);
                    } else if (endAt instanceof Boolean) {
                        query = query.endAt((Boolean)endAt, key);
                    } else {
                        query = query.endAt(endAt.toString(), key);
                    }
                } else if (equalTo != null) {

                    if (equalTo instanceof Number) {
                        if (key == null) {
                            query = query.equalTo((Double)equalTo);
                        } else {
                            query = query.equalTo((Double)equalTo, key);
                        }
                    } else if (equalTo instanceof Boolean) {
                        if (key == null) {
                            query = query.equalTo((Boolean)equalTo);
                        } else {
                            query = query.equalTo((Boolean)equalTo, key);
                        }
                    } else {
                        String value = equalTo.toString();
                        if (key == null) {
                            query = query.equalTo(value);
                        } else {
                            query = query.equalTo(value, key);
                        }
                    }
                } else {
                    throw new JSONException("includes are invalid");
                }
            }

            if (limit != null) {
                if (limit.has("first")) {
                    query = query.limitToFirst(limit.getInt("first"));
                } else if (limit.has("last")) {
                    query = query.limitToLast(limit.getInt("last"));
                }
            }
        }

        return query;
    }

    private static FirebaseDatabase getDb(String url) {
        if (url.isEmpty()) {
            return FirebaseDatabase.getInstance();
        } else {
            return FirebaseDatabase.getInstance(url);
        }
    }

    private PluginResult createPluginResult(DataSnapshot dataSnapshot, boolean keepCallback) {
        JSONObject data = new JSONObject();
        Object value = dataSnapshot.getValue(false);
        try {
            data.put("priority", dataSnapshot.getPriority());
            data.put("key", dataSnapshot.getKey());
            if (value instanceof Map) {
                value = new JSONObject(this.gson.toJson(value));
            } else if (value instanceof List) {
                value = new JSONArray(this.gson.toJson(value));
            }
            data.put("value", value);
            JSONArray jsonArray = new JSONArray();
            for (DataSnapshot postSnapshot: dataSnapshot.getChildren()) {
                JSONObject myJsonObject = new JSONObject();
                myJsonObject.put("key", postSnapshot.getKey());
                myJsonObject.put("value", postSnapshot.getValue());
                jsonArray.put(myJsonObject);
            }
            data.put("children", jsonArray);
        } catch (JSONException e) {}

        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, data);
        pluginResult.setKeepCallback(keepCallback);
        return pluginResult;
    }

    private Object toSettable(Object value) {
        Object result = value;

        if (value instanceof JSONObject) {
            result = this.gson.fromJson(value.toString(), settableTypeMap);
        } else if (value instanceof JSONArray) {
            result = this.gson.fromJson(value.toString(), settableTypeList);
        } else if (value == JSONObject.NULL) {
            result = null;
        }

        return result;
    }
}
