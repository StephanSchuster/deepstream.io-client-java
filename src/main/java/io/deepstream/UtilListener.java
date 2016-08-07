package io.deepstream;

import io.deepstream.constants.Actions;
import io.deepstream.constants.Topic;

class UtilListener implements UtilResubscribeNotifier.UtilResubscribeListener {

    private final Topic topic;
    private final DeepstreamConfig deepstreamConfig;
    private final UtilResubscribeNotifier resubscribeNotifier;

    private UtilAckTimeoutRegistry ackTimoutRegistry;
    private String pattern;
    private ListenListener listenerCallback;
    private DeepstreamClientAbstract client;
    private IConnection connection;

    public UtilListener(Topic topic, String pattern, ListenListener listenerCallback, DeepstreamConfig deepstreamConfig, DeepstreamClientAbstract client, IConnection connection) {
        this.topic = topic;
        this.pattern = pattern;
        this.listenerCallback = listenerCallback;
        this.deepstreamConfig = deepstreamConfig;
        this.client = client;
        this.connection = connection;
        this.resubscribeNotifier = new UtilResubscribeNotifier( this.client, this );
        this.ackTimoutRegistry = client.getAckTimeoutRegistry();
        this.scheduleAckTimeout();
        this.sendListen();
    }

    public void destroy() {
        this.connection.sendMsg( this.topic, Actions.UNLISTEN, new String[] { this.pattern } );
        this.resubscribeNotifier.destroy();
        this.listenerCallback = null;
        this.pattern = null;
        this.client = null;
        this.connection = null;
        this.ackTimoutRegistry = null;
    }

    public void onMessage( Message message ) {
        if( message.action.equals( Actions.ACK ) ) {
            this.ackTimoutRegistry.clear( message );
        } else {
            boolean isFound = message.action.equals( Actions.SUBSCRIPTION_FOR_PATTERN_FOUND);
            if( isFound ) {
                this.listenerCallback.onSubscriptionForPatternAdded( message.data[ 1 ] );
            } else {
                this.listenerCallback.onSubscriptionForPatternRemoved( message.data[ 1 ] );
            }

        }
    }

    private void sendListen() {
        this.connection.sendMsg( this.topic, Actions.LISTEN, new String[] { this.pattern } );
    }

    private void scheduleAckTimeout() {
        int subscriptionTimeout = deepstreamConfig.getSubscriptionTimeout();
        this.ackTimoutRegistry.add( this.topic, Actions.LISTEN, this.pattern, subscriptionTimeout );
    }

    @Override
    public void resubscribe() {
        this.scheduleAckTimeout();
        sendListen();
    }
}
