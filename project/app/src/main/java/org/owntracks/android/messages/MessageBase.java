package org.owntracks.android.messages;
import android.databinding.BaseObservable;
import android.support.annotation.NonNull;

import org.owntracks.android.support.IncomingMessageProcessor;
import org.owntracks.android.support.OutgoingMessageProcessor;
import org.owntracks.android.support.PausableThreadPoolExecutor;

import java.lang.ref.WeakReference;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "_type", defaultImpl = MessageUnknown.class)
@JsonSubTypes({
        @JsonSubTypes.Type(value=MessageLocation.class, name="location"),
        @JsonSubTypes.Type(value=MessageTransition.class, name="transition"),
        @JsonSubTypes.Type(value=MessageEvent.class, name="event"),
        @JsonSubTypes.Type(value=MessageCard.class, name="card"),
        @JsonSubTypes.Type(value=MessageCmd.class, name="cmd"),
        @JsonSubTypes.Type(value=MessageConfiguration.class, name="configuration"),
        @JsonSubTypes.Type(value=MessageEncrypted.class, name="encrypted"),
        @JsonSubTypes.Type(value=MessageWaypoint.class, name="waypoint"),
        @JsonSubTypes.Type(value=MessageWaypoints.class, name="waypoints")
})

public abstract class MessageBase extends BaseObservable implements PausableThreadPoolExecutor.ExecutorRunnable {
        protected static final String TAG = "MessageBase";

        @JsonIgnore
        private String _mqtt_topic;

        @JsonIgnore
        public long getMessageId() {
                return _messageId;
        }

        @JsonIgnore
        private Long _messageId = System.currentTimeMillis();

        @JsonIgnore
        private int _outgoingTTL = 2;

        @JsonIgnore
        public int getOutgoingTTL() {
                return _outgoingTTL;
        }


        @JsonIgnore
        private int _mqtt_qos;

        @JsonIgnore
        private boolean _mqtt_retained;
        private volatile boolean cancelOnRun = false;
        private int direction = DIRECTION_INCOMING;
        private static final int DIRECTION_INCOMING = 1;
        private static final int DIRECTION_OUTGOING = 2;
        private String tid;

        @JsonIgnore
        public boolean getRetained() {
                return _mqtt_retained;
        }

        @JsonIgnore
        public void setRetained(boolean _mqtt_retained) {
                this._mqtt_retained = _mqtt_retained;
        }
        @JsonIgnore
        public int getQos() {
                return _mqtt_qos;
        }

        @JsonIgnore
        public void setQos(int _mqtt_qos) {
                this._mqtt_qos = _mqtt_qos;
        }

        @JsonIgnore
        private WeakReference<IncomingMessageProcessor> _processorIn;

        @JsonIgnore
        private WeakReference<OutgoingMessageProcessor> _processorOut;

        @JsonIgnore
        @NonNull
        public String getContactKey() {
                if(_mqtt_topic != null)
                        return _mqtt_topic;
                if(tid != null)
                        return tid;
                return
                        "NOKEY";
        }

        public String getTopic() {
                return _mqtt_topic;
        }

        @JsonIgnore
        public void setTopic(String _topic) {
                this._mqtt_topic = _topic;
        }

        @Override
        public void run(){

                // If the message is enqueued to a ThreadPoolExecutor, stopping that executor results in the first queued message runnable being run
                // We check if the running thread is shutting down and don't submit that messagfe to the message handler
                if(cancelOnRun)
                        return;

                if(_processorIn != null && _processorIn.get() !=  null)
                        processIncomingMessage(_processorIn.get());
                if(_processorOut != null && _processorOut.get() !=  null) {
                        _outgoingTTL --;
                        processOutgoingMessage(_processorOut.get());
                }
        }

        @Override
        public void cancelOnRun() {
                this.cancelOnRun = true;
        }


        @JsonIgnore
        public void setIncomingProcessor(IncomingMessageProcessor processor) {
                this._processorIn = new WeakReference<>(processor);
        }

        @JsonIgnore
        public void setOutgoingProcessor(OutgoingMessageProcessor processor) {
                this._processorOut = new WeakReference<>(processor);
        }

        @JsonIgnore
        protected abstract void processIncomingMessage(IncomingMessageProcessor handler);

        @JsonIgnore
        protected abstract void processOutgoingMessage(OutgoingMessageProcessor handler);

        @JsonIgnore
        public abstract String getBaseTopicSuffix();

        // Called after deserialization to check if all required attributes are set or not.
        // The message is discarded if false is returned.
        @JsonIgnore
        public boolean isValidMessage() {
                return true;
        }

        @JsonIgnore
        public void setIncoming() {
                this.direction = DIRECTION_INCOMING;
        }

        @JsonIgnore
        public void setOutgoing() {
                this.direction = DIRECTION_OUTGOING;

        }

        @JsonIgnore
        public boolean isIncoming() {
                return this.direction == DIRECTION_INCOMING;
        }

        @JsonIgnore
        public boolean isOutgoing() {
                return !isIncoming();
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public String getTid() {
                return tid;
        }

        @JsonIgnore
        public boolean hasTid() {
                return getTid() != null;
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        public void setTid(String tid) {
                this.tid = tid;
        }
}
