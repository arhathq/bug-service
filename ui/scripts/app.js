// an observer for when the socket is open
var openObserver = Rx.Observer.create(function(e) {
    console.info('socket open');

    // Now it is safe to send a message
    socket.onNext('5');
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
        var slaDatasets = report['sla-chart'].datasets;
        var bugsOutSlaDatasets = report['out-sla-bugs-chart'].datasets;
        var weeklySummaryDatasets = report['week-summary-report-chart'].datasets;

        console.log('json: %s', slaDatasets);
        console.log('labels: %s', slaDatasets.labels);
        console.log('message: %s', e.data);

        BugApp.Charts.renderSlaChart(slaChart1, slaDatasets, 0);
        BugApp.Charts.renderSlaChart(slaChart2, slaDatasets, 1);
        BugApp.Charts.renderBugsOutSlaChart(bugsOutSlaChart, bugsOutSlaDatasets);
        BugApp.Charts.renderWeeklyChart(weeklySummaryChart, weeklySummaryDatasets);

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

BugApp = {

};

BugApp.Charts = {

    bgColors: [
        'rgba(255, 99, 132, 0.2)',
        'rgba(54, 162, 235, 0.2)',
        'rgba(255, 206, 86, 0.2)',
        'rgba(75, 192, 192, 0.2)',
        'rgba(153, 102, 255, 0.2)',
        'rgba(255, 159, 64, 0.2)',
        'rgba(000, 128, 000, 0.2)'
    ],

    brdColors: [
        'rgba(255,99,132,1)',
        'rgba(54, 162, 235, 1)',
        'rgba(255, 206, 86, 1)',
        'rgba(75, 192, 192, 1)',
        'rgba(153, 102, 255, 1)',
        'rgba(255, 159, 64, 1)',
        'rgba(000, 128, 000, 1)'
    ],

    _initSlaChart: function(ctx) {

    },

    renderSlaChart: function(chart, data, idx) {
        chart.data.labels = data.labels;
        let dss = [];
        for (let i = idx; i < idx + 1; i++) {
            let legend = data.dataset[i].name;
            let values = data.dataset[i].values;
            dss.push({
                label: legend,
                data: values,
                backgroundColor: [ BugApp.Charts.bgColors[i] ],
                borderColor: [ BugApp.Charts.brdColors[i] ],
                borderWidth: 2
            });
        }
        chart.data.datasets = dss;
        chart.update();
    },

    renderBugsOutSlaChart: function(chart, data, idx1, idx2) {
        chart.data.labels = data.labels;
        let dss = [];
        for (let i = 0; i < data.dataset.length; i++) {
            let legend = data.dataset[i].name;
            let values = [];
            let bgColors = [];
            let brdColors = [];
            console.log("dataset count: %i", data.dataset.length);
            for (let j = 0; j < data.dataset[i].values.length; j++) {
                values.push(data.dataset[i].values[j].y);
                bgColors.push(BugApp.Charts.bgColors[i]);
                brdColors.push(BugApp.Charts.brdColors[i]);
            }
            console.log("Values: %i %o", i, values);
            console.log('%i, %s, %s', i, bgColors, brdColors);
            dss.push({
                label: legend,
                data: values,
                backgroundColor: bgColors,
                borderColor: brdColors,
                borderWidth: 2
            });
        }
        chart.data.datasets = dss;
        chart.update();
    },

    renderWeeklyChart: function(chart, data) {
        var _bgColors = [
            'rgba(200, 000, 000, 0.2)',
            'rgba(000, 128, 000, 0.2)'
        ];

        var _brdColors = [
            'rgba(200, 000, 000, 1)',
            'rgba(000, 128, 000, 1)'
        ];

        chart.data.labels = data.labels;
        let dss = [];
        for (let i = 0; i < data.dataset.length; i++) {
            let legend = data.dataset[i].name;
            let values = [];
            let bgColors = [];
            let brdColors = [];
            console.log("dataset count: %i", data.dataset.length);
            for (let j = 0; j < data.dataset[i].values.length; j++) {
                values.push(data.dataset[i].values[j].y);
                bgColors.push(_bgColors[i]);
                brdColors.push(_brdColors[i]);
            }
            console.log("Values: %i %o", i, values);
            console.log('%i, %s, %s', i, bgColors, brdColors);
            dss.push({
                label: legend,
                data: values,
                backgroundColor: bgColors,
                borderColor: brdColors,
                borderWidth: 2
            });
        }
        chart.data.datasets = dss;
        chart.update();
    }

};