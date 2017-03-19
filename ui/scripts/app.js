// an observer for when the socket is open
var openObserver = Rx.Observer.create(function(e) {
    console.info('socket open');

    // Now it is safe to send a message
    socket.onNext('15');
});

// an observer for when the socket is about to close
var closingObserver = Rx.Observer.create(function() {
    console.log('socket is about to close');
});

// create a web socket subject
socket = Rx.DOM.fromWebSocket(
    'ws://localhost:9000/api/online/sla',
    null, // no protocol
    openObserver,
    closingObserver);

// subscribing creates the underlying socket and will emit a stream of incoming
// message events
socket.subscribe(
    function(e) {
        var json = JSON.parse(e.data);
        var report = json['bug-reports'];
        console.log('json: %s', report['out-sla-bugs']);
        console.log('message: %s', e.data);
    },
    function(e) {
        // errors and "unclean" closes land here
        console.error('error: %s', e);
    },
    function() {
        // the socket has been closed
        console.info('socket closed');
    }
);

var myChart = new Chart();