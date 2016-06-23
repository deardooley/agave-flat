<html>
<head>
    <title>Agave Jobs API Worker Manager</title>
    <script type="text/javascript" src="http://code.jquery.com/jquery-1.10.2.min.js"></script>
    <script src="http://code.jquery.com/ui/1.10.4/jquery-ui.min.js"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/moment.js/2.10.3/moment.min.js"></script>

    <link href="http://code.jquery.com/ui/1.10.4/themes/smoothness/jquery-ui.css" rel="stylesheet">

    <style>
        .ui-dialog-title {
            font-size: 16px;
            text-transform: capitalize;
        }

        .ui-button-text-only {
            padding: 3px;

        }

        .cronInput {
            border-color: red;
            border: .2;
            border-style: dotted;
            border-radius: 2px;
            display: none;
            width: 200px
        }

        h5 {
            margin-top: 0px;
            font-size: 16px;
            text-transform: capitalize;
        }

        dl {
            margin: 7px 0;
        }

        dl dt {
            display: inline-block;
            font-weight: bold;
            text-transform: capitalize;
            width: 10%;
            font-size: 14px;
            vertical-align: top;
            margin: 7px 0;
        }

        dl dd {
            display: inline-block;
            width: 83%;
            text-align: left;
            font-size: 14px;
            vertical-align: top;
            margin: 7px 0;
        }


    </style>
    <script type="text/javascript">
    	  (function($) {
  		    $.QueryString = (function(a) {
  		        if (a == "") return {};
  		        var b = {};
  		        for (var i = 0; i < a.length; ++i)
  		        {
  		            var p=a[i].split('=');
  		            if (p.length != 2) continue;
  		            b[p[0]] = decodeURIComponent(p[1].replace(/\+/g, " "));
  		        }
  		        return b;
  		    })(window.location.search.substr(1).split('&'))
		    })(jQuery);
        /**
         *
         * url configuration.
         */
        /**
         * Created by dooley on 5/20/15.
         */
        var _tools = {
            listJobsURL: 'listJobs.quartz?scheduler='+ $.QueryString["scheduler"],
            listChangesURL: 'listChanges.quartz?scheduler='+ $.QueryString["scheduler"],
            resumeAllURL: 'resumeAll.quartz?scheduler='+ $.QueryString["scheduler"],
            pauseAllURL: 'pauseAll.quartz?scheduler='+ $.QueryString["scheduler"],
            pauseJobURL: 'pauseJob.quartz?scheduler='+ $.QueryString["scheduler"],
            resumeJobURL: 'resumeJob.quartz?scheduler='+ $.QueryString["scheduler"],
            interruptJobURL: 'interruptJob.quartz?scheduler='+ $.QueryString["scheduler"],
            pauseTriggerURL: 'pauseTrigger.quartz?scheduler='+ $.QueryString["scheduler"],
            resumeTriggerURL: 'resumeTrigger.quartz?scheduler='+ $.QueryString["scheduler"],
            revertToInitialTriggerURL: 'revertChange.quartz?scheduler='+ $.QueryString["scheduler"],
            quartzChangeURL: 'quartzChange.quartz?scheduler='+ $.QueryString["scheduler"]
        };

        if (!window.jQuery) {
            document.write('can\'t load jquery');
        } else {
            /**
             * load stuff.
             */
            $(document).ready(
                    function () {
                        init();
                    }
            );
        }

        /**
         * creates the widgets, sets various stuff to default states.
         **/
        function init() {

            $('#allChangesLog').dialog({modal: true, autoOpen: false, height: 200, width: 800});

            $('#loading, #success, #error').hide();

            $(document).bind("ajaxSend", function () {
                $('#loading').fadeIn();
            }).bind("ajaxComplete", function (evt, xhr) {
                if (xhr.responseJSON && xhr.responseJSON.type) {
                    if (xhr.responseJSON.type == 'ERROR') {
                        $('#error').fadeIn();
                        $('#error p').text(xhr.responseJSON.message);
                    }
                }
                $('#loading').hide();
            });

            // intial quartz state
            listJobs();
        }


        /**
         * TODO : use
         * tests the expression
         * @param cronStr
         * @returns {boolean}
         */
        function validateCron(cronStr) {
            var re = /^([0-9]|[12]\d|5[0-9])|([0-9]|[12]\d|5[0-9])\-([0-9]|[12]\d|5[0-9])([0-9]|[12]\d|5[0-9])|([0-9]|[12]\d|5[0-9])\/([0-9]|[12]\d|5[0-9])([0-9]|[12]\d|5[0-9])|(\*)|(\?)/g;
            return re.test(cronStr);
        }

        /**
         *  toggles cron view/edit element
         *
         * @param cronElement
         * @param target  trigger id
         * @param readonly view/edit
         */
        function toggleEditExpression(cronElement, target, readonly) {

            var edits = [$('#' + 'r' + target), $('#' + 'cancelButton' + target), $('#' + 'changeButton' + target), $('#' + target)];
            if (readonly == true) {
                $(edits).each(function (id) {
                    edits[id].fadeOut(10);
                });

                $('#r' + target).fadeIn(10);
                return;
            }

            $(cronElement).fadeOut(10);

            $('#' + target).val($(cronElement).html());
            $('#' + target).fadeIn(10);

            $('#' + 'changeButton' + target).fadeIn();
            $('#' + 'cancelButton' + target).fadeIn();

            $('#' + 'changeButton' + target).css('width', 500);

        }

        /**
         * shows the change log.
         */
        function viewLogs() {
            $('#allChangesLog').dialog("open");
            listChanges();
        }

        /**
         * sends request to change the job trigger
         *
         * @param caller
         */
        function changeJob(caller) {
            $.ajax({
                method: "POST",
                url: _tools.quartzChangeURL,
                dataType: "json",
                data: {
                    target: $(caller).attr('jobName'),
                    trigger: $(caller).attr('triggerId'),
                    newExpression: $('#' + $(caller).attr('newValueElement')).val(),
                    oldExpression: $(caller).attr('oldvalue')
                },
                success: function (res) {
                    toggleEditExpression($('#changeButton'), $(caller).attr('triggerId'), true);
                }
            });
        }

        /**
         * sends request to resume all jobs
         *
         * @param caller
         */
        function resumeAllJobs() {
            $.ajax({
                method: "POST", url: _tools.resumeAllURL, dataType: "json", success: function () {
                    window.location.reload();
                }
            });
        }

        /**
         * sends request to pause all jobs
         *
         * @param caller
         */
        function pauseAllJobs() {
            $.ajax({
                method: "POST", url: _tools.pauseAllURL, dataType: "json", success: function () {
                    window.location.reload();
                }
            });
        }

        /**
         * sends request to pause job
         *
         * @param caller
         */
        function pauseJob(jobName) {
            $.ajax({
                method: "POST", url: _tools.pauseJobURL, data: {target: jobName}, dataType: "json",
                success: function () {
                    window.location.reload();
                }
            });
        }

        /**
         * sends request to resume paused job
         *
         * @param caller
         */
        function resumeJob(jobName) {
            $.ajax({
                method: "POST", url: _tools.resumeJobURL, data: {target: jobName}, dataType: "json",
                success: function () {
                    window.location.reload();
                }
            });
        }

        /**
         * sends request to interrupt job
         *
         * @param caller
         */
        function interruptJob(jobName) {
            $.ajax({
                method: "POST", url: _tools.interruptJobURL, data: {target: jobName}, dataType: "json",
                success: function () {
                    window.location.reload();
                }
            });
        }

        /**
         * sends request to pause trigger
         *
         * @param caller
         */
        function pauseTrigger(triggerName, triggerGroup) {
            $.ajax({
                method: "POST",
                url: _tools.pauseTriggerURL,
                data: {triggerName: triggerName, triggerGroup: triggerGroup},
                dataType: "json",
                success: function () {
                    window.location.reload();
                }
            });
        }

        /**
         * sends request to resume trigger
         *
         * @param caller
         */
        function resumeTrigger(triggerName, triggerGroup) {
            $.ajax({
                method: "POST",
                url: _tools.resumeTriggerURL,
                data: {triggerName: triggerName, triggerGroup: triggerGroup},
                dataType: "json",
                success: function () {
                    window.location.reload();
                }
            });
        }

        /**
         * sends request pause job
         *
         * @param caller
         */
        function restoreTrigger(triggerName, triggerGroup) {
            $.ajax({
                method: "POST",
                url: _tools.revertToInitialTriggerURL,
                data: {trigger: triggerName},
                dataType: "json",
                success: function () {
                    window.location.reload();
                }
            });
        }

        /**
         * lists all changes
         *
         * @param caller
         */
        function listChanges() {
            $.ajax({
                method: "GET",
                url: _tools.listChangesURL,
                dataType: "json",
                success: function (response) {
                    $(response).each(function (idx) {
                        var momentum = new Date(response[idx].momentum);
                        $('#logsContent').append(
                                $('#jobChangesLogLineTemplate').html().format(
                                        "" + momentum.getDate() + "/" + momentum.getMonth() + "/" + momentum.getFullYear() + "-" + momentum.getHours() + ":" + momentum.getMinutes() + ":" + momentum.getSeconds(),
                                        response[idx].type,
                                        response[idx].message
                                )
                        );
                    });
                }
            });
        }

        /**
         * triggers change class priority request.
         *
         */
        function listJobs() {
            $.ajax({
                method: "GET",
                url: _tools.listJobsURL,
                dataType: "json",
                success: function (res) {
                    var jobAcc = $('#jobsAcc');
                    jobAcc.append($('#schedulerDetailsTemplate').html().format(
                            res.details.version
                            , res.details.runningSince
                            , res.details.jobsExecuted
                            , res.details.summary
                            , res.details.pausedTriggers
                            , res.details.pausedJobs
                    ));

                    var data = res.jobs;
                    $(data).each(function (idx) {
                        jobAcc.append(
                                $('#jobAccTemplate').html().format(
                                        data[idx].name
                                        , (data[idx].cronExpressions.length ?
                                                (data[idx].cronExpressions[0].id + " ( triggers : " + (data[idx].cronExpressions.length > 0 ? data[idx].cronExpressions.length : "") + " )") :
                                                'No cron expressions/triggers found')
                                        , $('#jobScheduleTemplate').html().format(
                                                data[idx].className
                                                , data[idx].name
                                                , data[idx].description
                                                , data[idx].groupName
                                                , renderTriggers(data[idx])
                                                , data[idx].paused
                                                , renderActiveJobs(data[idx])
                                        )
                                ));
                    });

                    function renderTriggers(job) {
                        console.log('renderTriggers(job)', job);
                        var trVal = '';
                        $(job.cronExpressions).each(
                                function (jdx) {
                                    trVal = trVal + $('#triggerTemplate').html().format(
                                                    job.cronExpressions[jdx].id.replace(/[\s]|[\.]/g, '')
                                                    , job.cronExpressions[jdx].expression
                                                    , job.name
                                                    , jdx
                                                    , job.cronExpressions[jdx].id
                                                    , job.groupName
                                                    , job.cronExpressions[jdx].paused
                                            );
                                });

                        return trVal;
                    }

                    function renderActiveJobs(job) {
                        console.log('renderActiveJobs(job)', job);
                        var ajVal = '';
                        $(job.activeJobDescriptions).each(
                                function (ajd) {
                                    ajVal = ajVal + $('#activeJobDescriptionTemplate').html().format(
                                                    this.isActive
                                                    , JSON.stringify(JSON.parse(this.description))
                                                    , moment(this.scheduledAt).toString()
                                                    , moment(this.firedAt).toString()
                                                    , moment(this.nextFireAt).toString()
                                                    , moment(this.previousFiredAt).toString()
                                                    , this.refireCount
                                                    , this.isRecovering
                                                    , this.lastFireDuration
                                                    , (this.allowsConcurrentExecution > 0)
                                                    , this.result
                                                    , this.triggerKey
                                            );
                                });

                        return ajVal;
                    }

                    function decorateCron(cronStr) {
                        return cronStr;
                        var decorated = '';
                        $(cronStr.split(" ")).each(function (idx) {
                            decorated = decorated + cronStr[idx];
                        });
                        console.log(cronStr.split(" "));
                        return decorated;
                    }

                    jobAcc.accordion();

                },
                error: function (data) {
                    $('#error').html(data.responseText);
                    $('#error').fadeIn();
                }
            });
        }

    </script>

    <script type="text/javascript">

        function describe() {
            var obj = [];
            //'(a, b, c, d, e, f)'
            var tmp = arguments.callee.toString().match(/\(.*?\)/)[0];
            //["a", "b", "c", "d", "e", "f"]
            var argumentNames = tmp.replace(/[()\s]/g, '').split(',');

            [].splice.call(arguments, 0).forEach(function (arg, i) {
                obj.push({
                    // question is how to get variable name here?
                    name: argumentNames[i],
                    value: arg
                })
            });
            return obj;
        }

        String.prototype.format = function () {
            var args = arguments;
            return this.replace(/{(\d+)}/g, function (match, number) {
                return typeof args[number] != 'undefined'
                        ? args[number]
                        : match
                        ;
            });
        };
    </script>

<head>
<body style="font-family: Courier;">
<div id="container">


    <div class="ui-widget ui-state-highlight ui-corner-all">
        <div id="jobsAcc" class="ui-accordion ui-widget ui-helper-reset" role="tablist">

        </div>

        <div id="schedulerDetailsTemplate" style="display: none;">
            <h4>quartz jobs tools</h4>

            <div>
                <h5>quartz info</h5>
                <dl>
                    <dt>version :</dt>
                    <dd>{0}</dd>

                    <dt>runningSince :</dt>
                    <dd>{1}</dd>

                    <dt>paused jobs:</dt>
                    <dd>{5}</dd>

                    <dt>paused trigger groups:</dt>
                    <dd>{4}</dd>

                    <dt>jobsExecuted :</dt>
                    <dd>{2}</dd>

                    <dt>summary :</dt>
                    <dd>{3}</dd>

                    <dt>actions :</dt>
                    <dd>
                        <button id="pauseAll"
                                class="ui-button ui-widget ui-state-default ui-corner-all ui-button-text-only"
                                onclick="pauseAllJobs()"
                                role="button" aria-disabled="false">
                            pause all jobs
                        </button>
                        <button id="resumeAll"
                                class="ui-button ui-widget ui-state-default ui-corner-all ui-button-text-only"
                                onclick="resumeAllJobs()"
                                role="button" aria-disabled="false">
                            resume all jobs
                        </button>
                    </dd>

                    <dt>view changes:</dt>
                    <dd>
                        <button id="allChanges"
                                class="ui-button ui-widget ui-state-default ui-corner-all ui-button-text-only"
                                onclick="viewLogs()"
                                role="button" aria-disabled="false">
                            view all job/trigger changes
                        </button>
                    </dd>
                </dl>


            </div>
        </div>

        <div id="jobAccTemplate" style="display: none;">
            <h3> {0} - {1}</h3>

            <div>
                <p> {2} </p>
            </div>
        </div>

        <div id="jobChangesLogLineTemplate" style="display: none;">
            <div style="padding:2px;">
                <span style="float: left; width: 15%;">{0}</span> <span
                    style="font-weight: bolder;width: 5%;">[{1}]</span>
                : {2}
            </div>
        </div>

        <div id="jobScheduleTemplate" style="display:none;">
            <div>
                <dl>
                    <dt>class name</dt>
                    <dd>{0}</dd>

                    <dt>job name</dt>
                    <dd>{1}</dd>

                    <dt>is paused</dt>
                    <dd><b>{5}</b></dd>

                    <dt>group</dt>
                    <dd>{2}, <strong>group</strong> : {3}</dd>

                    <dt>job actions</dt>
                    <dd>
                        <button id="pauseJobButton"
                                class="ui-button ui-widget ui-state-default ui-corner-all ui-button-text-only"
                                onclick="pauseJob('{1}')"
                                role="button" aria-disabled="false">pause job
                        </button>
                        <button id="resumeJobButton"
                                class="ui-button ui-widget ui-state-default ui-corner-all ui-button-text-only"
                                onclick="resumeJob('{1}')"
                                role="button" aria-disabled="false">resume job
                        </button>
                        <button id="interruptButton"
                                class="ui-button ui-widget ui-state-default ui-corner-all ui-button-text-only"
                                onclick="interruptJob('{1}')"
                                role="button" aria-disabled="false">interrupt job
                        </button>
                    </dd>

                    <dt>cron expressions</dt>
                    <dd>{4}</dd>

                    <dt>active instances</dt>
                    <dd>{6}</dd>

                </dl>
            </div>

        </div>

        <div id="activeJobDescriptionTemplate" style="display:none;">
            <div>
                <dl>
                    <dt>Is active</dt>
                    <dd>{0}</dd>

                    <dt>Description</dt>
                    <dd>{1}</dd>

                    <dt>Scheduled at</dt>
                    <dd>{2}</dd>

                    <dt>Fired at</dt>
                    <dd>{3}</dd>

                    <dt>Next fire at</dt>
                    <dd>{4}</dd>

                    <dt>Previous fire at</dt>
                    <dd>{5}</dd>

                    <dt>Refire count</dt>
                    <dd>{6}</dd>

                    <dt>Last fire duration</dt>
                    <dd>{7}</dd>

                    <dt>Allows concurrent execution</dt>
                    <dd>{8}</dd>

                    <dt>Result</dt>
                    <dd>{9}</dd>

                    <dt>Trigger key</dt>
                    <dd>{10}</dd>
                </dl>
            </div>

        </div>

        <div id="triggerTemplate" style="display: none;">
            <div style="padding-top: 10px;">
                <div style="padding: 10px;">trigger id : {4} (paused : <b>{6}</b>)</div>
                <a id="r{0}" style="padding:10px;" title="click to edit" href="#"
                   onclick="toggleEditExpression(this,'{0}');$('\\#{0}').focus()">{1}</a>
                <input id="{0}" type="text" class="cronInput"/>
                    <span>
                       <span id="changeButton{0}" style="display: none;">
                            <button id="changeButton"
                                    class="ui-button ui-widget ui-state-default ui-corner-all ui-button-text-only"
                                    onclick="changeJob(this)"
                                    newValueElement="{0}"
                                    oldvalue="{1}"
                                    jobName="{2}"
                                    triggerId="{4}"
                                    role="button" aria-disabled="false">submit change
                            </button>
                        </span>
                       <span id="cancelButton{0}" style="display: none;">
                            <button
                                    class="ui-button ui-widget ui-state-default ui-corner-all ui-button-text-only"
                                    oldvalue="{1}"
                                    jobName="{2}"
                                    triggerId="{4}"
                                    onclick="toggleEditExpression(this,'{0}', true);"
                                    role="button" aria-disabled="false">cancel edit
                            </button>
                        </span>
                        <span>
                             <button id="pauseTriggerButton"
                                     class="ui-button ui-widget ui-state-default ui-corner-all ui-button-text-only"
                                     onclick="pauseTrigger('{4}','{5}')"
                                     newValueElement="{0}"
                                     oldvalue="{1}"
                                     jobName="{2}"
                                     triggerId="{4}"
                                     role="button" aria-disabled="false">pause trigger
                             </button>
                        </span>
                        <span>
                             <button id="resumeTriggerButton"
                                     class="ui-button ui-widget ui-state-default ui-corner-all ui-button-text-only"
                                     onclick="resumeTrigger('{4}','{5}')"
                                     newValueElement="{0}"
                                     oldvalue="{1}"
                                     jobName="{2}"
                                     triggerId="{4}"
                                     groupName="{5}"
                                     role="button" aria-disabled="false">resume trigger
                             </button>
                        </span>
                        <span>
                             <button id="restoreButton"
                                     class="ui-button ui-widget ui-state-default ui-corner-all ui-button-text-only"
                                     onclick="restoreTrigger('{4}','{5}')"
                                     newValueElement="{0}"
                                     oldvalue="{1}"
                                     jobName="{2}"
                                     triggerId="{4}"
                                     groupName="{5}"
                                     role="button" aria-disabled="false">revert to original cron expression
                             </button>
                        </span>
                     </span>
            </div>
        </div>
    </div>


    <div id="allChangesLog" style="font-size: 10px;"
         title="all changes">
        <div id="logsContent"></div>
        <!-- all logs -->
    </div>

    <div id="loading" style="display: none;">
        <div class="ui-state-highlight ui-corner-all" style="margin-top: 20px; padding: 0 .7em;">
            <p><span class="ui-icon ui-icon-info" style="float: left; margin-right: .3em;"></span>
                loading ..
            </p>
        </div>
    </div>
    <div id="error" style="display: none;">
        <div class="ui-state-error ui-corner-all" style="margin-top: 20px; padding: 0 .7em;">
            <p><span id="errorDetails" class="ui-icon ui-icon-info" style="float: left; margin-right: .3em;"></span>
                error
            </p>
        </div>
    </div>
    <div id="success" style="display: none;">
        <div class="ui-state-highlight ui-corner-all" style="margin-top: 20px; padding: 0 .7em;">
            <p><span id="successDetails" class="ui-icon ui-icon-info"
                     style="float: left; margin-right: .3em;"></span>
                success
            </p>
        </div>
    </div>

</body>
</html>
