var current_view = "system-information";

$(document).ready(function() {
    tree_initialize();

    $(document).on("click", "#save-config-button", function(e) {
        call_swrouter_api("write-startup-configuration", function(data) {
            tree_reload();
            reload_view();
        });
        e.stopPropagation();
    });

});

function tree_initialize() {
    $(document).on("click", ".tmlink", function(e) {
        view_name = $(this).attr("_open_view");
        load_view(view_name);
        e.stopPropagation();
    });

    tree_reload();
    load_view(current_view);
}

function tree_reload() {
    var tmhtml = "<ul>";
    tmhtml += "<li><span><i class=\"glyphicon glyphicon-cloud\"></i> <span class=\"tmph\" id=\"tmph--hostname\" /></span>";
    tmhtml += "<ul>";

    // System Information
    tmhtml += "<li><a href=\"#\" class=\"tmlink\" _open_view=\"system-information\"><span><i class=\"glyphicon glyphicon-info-sign\"></i> System Information</span></a></li>";

    // Network Interfaces Submenu
    tmhtml += "<li><a href=\"#\" class=\"tmlink\" _open_view=\"network-interfaces\"><span><i class=\"glyphicon glyphicon-resize-horizontal\"></i> Network Interfaces</span></a></li>";

    // Address Resolution Protocol
    tmhtml += "<li><a href=\"#\" class=\"tmlink\" _open_view=\"arp\"><span><i class=\" glyphicon glyphicon-screenshot\"></i> Address Resolution Protocol</span></a></li>";
    // Routing Submenu
    tmhtml += "<li><span><i class=\"glyphicon glyphicon-random\"></i> IP Routing</span> <ul>";
    tmhtml += "<li style=\"display: none;\"><a href=\"#\" class=\"tmlink\" _open_view=\"routing-table\"><span><i class=\"glyphicon glyphicon-list\"></i> Routing Table</span></a></li>";
    tmhtml += "<li style=\"display: none;\"><a href=\"#\" class=\"tmlink\" _open_view=\"routing-static\"><span><i class=\"glyphicon glyphicon-share-alt\"></i> Static Routes</span></a></li>";
    tmhtml += "<li style=\"display: none;\"><a href=\"#\" class=\"tmlink\" _open_view=\"routing-ripv2\"><span><i class=\"glyphicon glyphicon-road\"></i> RIPv2 Routing</span></a></li>";
    tmhtml += "</ul></li>";

    // Configuration Management
    tmhtml += "<li><a href=\"#\" class=\"tmlink\" _open_view=\"configuration-management\"><span><i class=\"glyphicon glyphicon-wrench\"></i> Configuration Management</span></a></li>";

    tmhtml += "</ul>";
    tmhtml += "</li>";
    tmhtml += "</ut>";
    $("#treemenu").html(tmhtml);
    $(function() {
        $('.tree li:has(ul)').addClass('parent_li').find(' > span').attr('title', 'Collapse this branch');
        $('.tree li.parent_li > span').on('click', function(e) {
            var children = $(this).parent('li.parent_li').find(' > ul > li');
            if (children.is(":visible")) {
                children.hide('fast');
                $(this).attr('title', 'Expand this branch').find(' > i').addClass('glyphicon-plus-sign').removeClass('glyphicon-minus-sign');
            } else {
                children.show('fast');
                $(this).attr('title', 'Collapse this branch').find(' > i').addClass('glyphicon-minus-sign').removeClass('glyphicon-plus-sign');
            }
            e.stopPropagation();
        });
    });

    call_swrouter_api("get-system-information", function(data) {
        tree_set_placeholder("hostname", data["Response"]["Hostname"]);
        if (data["Response"]["ConfigurationChanged"] === true) {
            $("#save-config-panel").fadeIn(500);
        } else {
            $("#save-config-panel").fadeOut(500);
        }
    });
}

function tree_set_placeholder(name, value) {
    $("#treemenu #tmph--" + name).html(value);
}

function load_view(view_name) {
    reset_view();
    data = "";
    switch (view_name) {
        case "system-information":
            {
                data = get_view_system_information();
                break;
            }
        case "routing-table":
            {
                data = get_view_routing_table();
                break;
            }
        case "network-interfaces":
            {
                data = get_view_network_interfaces();
                break;
            }
        case "configuration-management":
            {
                data = get_view_configuration_management();
                break;
            }
        case "arp":
            {
                data = get_view_arp();
                break;
            }
    }
    current_view = view_name;
    $("#content").html(data);
}

function reload_view() {
    load_view(current_view);
}

function reset_view() {

}













function get_view_system_information() {
    var view_html = "<placeholder identifier=\"system_info\" /><placeholder identifier=\"modules\" /><placeholder identifier=\"pipeline\" />";
    call_swrouter_api("get-system-information", function(data) {
        var html = "<h2><i class=\"glyphicon glyphicon-info-sign\"></i> System Information</h2>";
        html += "<table class=\"table\">";
        html += "<tbody>";
        html += "<tr><td><strong>Hostname</strong></td><td><a href=\"#\" id=\"hostname\">" + data["Response"]["Hostname"] + "</a></td></tr>";
        html += "<tr><td><strong>Uptime</strong></td><td>" + data["Response"]["Uptime"] + "</td></tr>";
        html += "<tr><td><strong>Boot Date</strong></td><td>" + data["Response"]["BootDate"] + "</td></tr>";
        html += "<tr><td><strong>Boot Time</strong></td><td>" + data["Response"]["BootTime"] + "</td></tr>";
        html += "<tr><td><strong>Interface Count</strong></td><td>" + data["Response"]["InterfaceCount"] + "</td></tr>";
        html += "</tbody>";
        html += "</table>";

        $("#content placeholder[identifier='system_info']").html(html);

        $("#content placeholder[identifier='system_info'] #hostname").editable({
            type: 'text',
            title: 'Enter Hostname',
            placement: "right",
            success: function(response, newValue) {
                call_swrouter_api_params("set-hostname", "hostname=" + newValue, function(data) {
                    if (data["Response"]["Success"] === true) {
                        tree_reload();
                    } else {
                        alert("Failed to change router hostname");
                    }
                });
            }
        });
    });

    call_swrouter_api("get-modules", function(data) {
        var html = "<h3><i class=\"glyphicon glyphicon-th\"></i> Modules</h3>";
        html += "<table class=\"table table-striped\">";
        html += "<thead><tr><th>Load Priority</th><th>Name</th><th>Revision</th><th>Classpath</th></tr></thead>";
        html += "<tbody>";
        for (var i in data["Response"]["Modules"]) {
            var obj = data["Response"]["Modules"][i];
            html += "<tr>";
            html += "<td>" + obj["LoadPriority"] + "</td>";
            html += "<td><strong>" + obj["Name"] + "</strong></td>";
            html += "<td>" + obj["Revision"] + "</td>";
            html += "<td>" + obj["Classpath"] + "</td>";
            html += "</tr>";
        }
        html += "</tbody>";
        html += "</table>";

        $("#content placeholder[identifier='modules']").html(html);
    });

    call_swrouter_api("get-processing-pipeline", function(data) {
        var html = "<h3><i class=\"glyphicon glyphicon-filter\"></i> Processing Pipeline</h3>";
        html += "<table class=\"table table-striped\">";
        html += "<thead><tr><th>Priority</th><th>Name</th><th>Enabled</th><th>Description</th><th>Classpath</th></tr></thead>";
        html += "<tbody>";
        for (var i in data["Response"]["PipelineBranches"]) {
            var obj = data["Response"]["PipelineBranches"][i];
            html += "<tr>";
            html += "<td>" + obj["Priority"] + "</td>";
            html += "<td><strong>" + obj["Name"] + "</strong></td>";
            html += "<td style=\"text-align: center;\">" + ((obj["Enabled"]) ? "<i class=\"text-success glyphicon glyphicon-ok\"></i>" : "<i class=\"text-danger glyphicon glyphicon-remove\"></i>") + "</td>";
            html += "<td>" + obj["Description"] + "</td>";
            html += "<td>" + obj["Classpath"] + "<br/><i>(installed by " + obj["InstallerClass"] + ")</i>" + "</td > ";
            html += "</tr>";
        }
        html += "</tbody>";
        html += "</table>";

        $("#content placeholder[identifier='pipeline']").html(html);
    });

    return view_html;
}




function get_view_network_interfaces() {
    var view_html = "<placeholder identifier=\"network_interfaces\" />";
    call_swrouter_api("get-network-interfaces", function(data) {
        var html = "<h3><i class=\"glyphicon glyphicon-resize-horizontal\"></i> Network Interfaces</h3>";
        html += "<table class=\"table table-striped\">";
        html += "<thead><tr><th width=\"16\"></th><th>Name</th><th>MTU</th><th>Hardware Address</th><th>IPv4 Address</th><th>Enabled</th></tr></thead>";
        html += "<tbody>";
        for (var i in data["Response"]["NetworkInterfaces"]) {
            var obj = data["Response"]["NetworkInterfaces"][i];
            html += "<tr>";
            html += "<td><i class=\"glyphicon glyphicon-resize-horizontal\"></i></td>";
            html += "<td><strong>" + obj["Name"] + "</strong></td>";
            html += "<td>" + obj["MTU"] + "</td>";
            html += "<td>" + obj["HardwareAddress"] + "</td > ";
            if (obj["IPv4Address"] === null) {
                html += "<td><i><a href=\"#\" class=\"nic_address\" _interface=\"" + obj["Name"] + "\">unconfigured</a></i></td > ";
            } else {
                html += "<td><a href=\"#\" class=\"nic_address\" _interface=\"" + obj["Name"] + "\">" + obj["IPv4Address"] + "/" + obj["IPv4NetworkMask"] + "</a></td > ";
            }
            html += "<td style=\"text-align: center;\"><a href=\#\" class=\"nic_enabled\" _interface=\"" + obj["Name"] + "\">" + ((obj["Enabled"]) ? "<i class=\"text-success glyphicon glyphicon-ok\"></i>" : "<i class=\"text-danger glyphicon glyphicon-remove\"></i>") + "</a></td>";
            html += "</tr>";
        }
        html += "</tbody>";
        html += "</table>";
        html += "<div class=\"alert alert-warning\" role=\"alert\"><strong>Address removal!</strong> In order to remote interface IPv4 address, set the value to empty or dash.</div>";

        $("#content placeholder[identifier='network_interfaces']").html(html);

        $(".nic_address").editable({
            type: "text",
            title: 'Enter IPv4 address and network mask',
            placement: "right",
            url: function(params) {
                var d = new $.Deferred;
                var nvspl = params.value.split("/");
                call_swrouter_api_params("configure-network-interface", "InterfaceName=" + $(this).attr("_interface") + "&IPv4Address=" + nvspl[0] + "&IPv4NetworkMask=" + nvspl[1], function(data) {
                    d.resolve(data);
                });
                return d.promise();
            },
            success: function(response, newValue) {
                if (response["UserError"] !== undefined) {
                    return response["UserError"];
                } else {
                    tree_reload();
                    reload_view();
                }
            }
        });

        $(".nic_enabled").click(function() {
            call_swrouter_api_params("configure-network-interface", "InterfaceName=" + $(this).attr("_interface") + "&Enabled=toggle", function(data) {
                if (data["UserError"] !== undefined) {
                    alert(data["UserError"]);
                } else {
                    tree_reload();
                    reload_view();
                }
            });
        });

    });
    return view_html;
}

function get_view_arp() {
    var view_html = "<h3><i class=\"glyphicon glyphicon-screenshot\"></i> Address Resolution Protocol</h3> <placeholder identifier=\"arp_config\" /> <placeholder identifier=\"arp_table\" /> <placeholder identifier=\"arp_va\" />";

    call_swrouter_api("get-arp-configuration", function(data) {
        var html = "<div class=\"panel panel-default\">"
                + "<div class=\"panel-heading\">"
                + "<h3 class=\"panel-title\"><i class=\"glyphicon glyphicon-wrench\"></i> Configuration</h3>"
                + "</div>"
                + "<div class=\"panel-body\"><table class=\"table table-striped\">";
        html += "<tr><td width=\"30%\"><strong>Entry Timeout</strong></td><td><a href=\"#\" id=\"arp-entry-timeout\">" + data["Response"]["ARPConfiguration"]["EntryTimeout"] + "ms</a></td></tr>";
        html += "<tr><td><strong>Pipeline Resolution Timeout</strong></td><td><a href=\"#\" id=\"arp-pipeline-resolution-timeout\">" + data["Response"]["ARPConfiguration"]["PipelineResolutionTimeout"] + "ms</a></td></tr>";
        html += "</table></div>";

        $("#content placeholder[identifier='arp_config']").html(html);

        $("#arp-entry-timeout").editable({
            type: "text",
            title: 'Enter ARP entry timeout',
            placement: "right",
            url: function(params) {
                var d = new $.Deferred;
                call_swrouter_api_params("configure-arp", "EntryTimeout=" + params.value, function(data) {
                    d.resolve(data);
                });
                return d.promise();
            },
            success: function(response, newValue) {
                if (response["UserError"] !== undefined) {
                    return response["UserError"];
                } else {
                    tree_reload();
                    reload_view();
                }
            }
        });

        $("#arp-pipeline-resolution-timeout").editable({
            type: "text",
            title: 'Enter ARP pipeline resolution timeout"',
            placement: "right",
            url: function(params) {
                var d = new $.Deferred;
                call_swrouter_api_params("configure-arp", "PipelineResolutionTimeout=" + params.value, function(data) {
                    d.resolve(data);
                });
                return d.promise();
            },
            success: function(response, newValue) {
                if (response["UserError"] !== undefined) {
                    return response["UserError"];
                } else {
                    tree_reload();
                    reload_view();
                }
            }
        });
    });

    call_swrouter_api("get-arp-table", function(data) {
        var html = "<h3><i class=\"glyphicon glyphicon-list\"></i> ARP Table</h3>";
        html += "<table class=\"table table-striped\">";
        html += "<thead><tr><th width=\"16\"></th><th>IPv4 Address</th><th>Network Interface</th><th>Hardware Address</th><th>Last Updated</th></tr></thead>";
        html += "<tbody>";
        for (var i in data["Response"]["ARPTable"]) {
            var obj = data["Response"]["ARPTable"][i];
            html += "<tr>";
            html += "<td><i class=\"glyphicon glyphicon-screenshot\"></i></td>";
            html += "<td><strong>" + obj["IPv4Address"] + "</strong></td>";
            html += "<td>" + obj["NetworkInterface"] + "</td>";
            if (obj.Complete) {
                html += "<td>" + obj["HardwareAddress"] + "</td>";
                html += "<td>" + moment(obj.LastUpdateTimestamp / 1000, "X").fromNow() + "</td>";
            } else {
                html += "<td><i>entry incomplete</i></td>";
                html += "<td>-</td>";
            }
            html += "</tr>";
        }
        html += "</tbody>";
        html += "</table>";

        $("#content placeholder[identifier='arp_table']").html(html);
    });

    return view_html;
}


function get_view_routing_table() {
    var view_html = "<placeholder identifier=\"routing_table\" />";

    call_swrouter_api("get-ip-routes", function(data) {
        var html = "<h3><i class=\"glyphicon glyphicon-list\"></i> IP Routing Table</h3>";
        html += "<table class=\"table table-striped\">";
        html += "<thead><tr><th width=\"16\"></th><th width=\"25\">Flags</th><th>Destination</th><th>Gateways</th><th>Administrative Distance</th></tr></thead>";
        html += "<tbody>";
        for (var i in data["Response"]["InstalledRoutes"]) {
            var obj = data["Response"]["InstalledRoutes"][i];
            html += "<tr>";
            html += "<td><i class=\"glyphicon glyphicon-share-alt\"></i></td>";
            html += "<td>" + obj["Flags"] + "</td>";
            html += "<td><strong>" + obj["TargetPrefix"] + "</strong></td>";

            var gwString = "";
            for (var j in obj["Gateways"]) {
                var obj2 = obj["Gateways"][j];
                if (gwString !== "") {
                    gwString += "<br />";
                }
                gwString += obj2;
            }
            html += "<td>" + gwString + "</td>";
            html += "<td>" + obj["AdministrativeDistance"] + "</td>";
            html += "</tr>";
        }
        html += "</tbody>";
        html += "</table>";

        $("#content placeholder[identifier='routing_table']").html(html);
    });

    return view_html;
}





function get_view_configuration_management() {
    var view_html = "<placeholder identifier=\"configuration_management\" />";

    call_swrouter_api("get-running-configuration", function(data) {
        var html = "<h3><i class=\"glyphicon glyphicon-wrench\"></i> Configuration Management</h3>";
        html += "<div style=\"width:100%; padding-bottom: 10px;\">";
        html += "<a class=\"btn btn-primary btn-sm\" href=\"#\" role=\"button\" id=\"download-config-button\"><i class=\"glyphicon glyphicon-download-alt\"></i> Download Running Configuration</a>";
        html += "</div>";
        html += "<div class=\"well\"><strong>Running Configuration</strong><pre class=\"json-syntax-highlight\">";
        html += json_syntax_highlight(JSON.stringify(data["Response"]["RunningConfiguration"], undefined, 4));
        html += "</pre></div>";

        $("#content placeholder[identifier='configuration_management']").html(html);

        $("#download-config-button").click(function() {
            location.href = "/api/get-running-configuration?download=1";
        });
    });

    return view_html;
}















function call_swrouter_api(call_name, callback) {
    $.post("/api/" + call_name, null, function(data) {
        if (data.Status !== true) {
            alert("ApiError: " + data.Error);
        } else {
            if (callback !== null) {
                callback(data);
            }
        }
    }, 'json');
}

function call_swrouter_api_params(call_name, post_data, callback) {
    $.post("/api/" + call_name, post_data, function(data) {
        if (data.Status !== true) {
            alert("ApiError: " + data.Error);
        } else {
            if (callback !== null) {
                callback(data);
            }
        }
    }, 'json');
}

function format_octet_size(bytes, precision, bps)
{
    var kilobyte = 1024;
    var megabyte = kilobyte * 1024;
    var gigabyte = megabyte * 1024;
    var terabyte = gigabyte * 1024;

    if ((bytes >= 0) && (bytes < kilobyte)) {
        return bytes + ((bps === true) ? " b/s" : " B");

    } else if ((bytes >= kilobyte) && (bytes < megabyte)) {
        return (bytes / kilobyte).toFixed(precision) + ((bps === true) ? " Kb/s" : " KB");

    } else if ((bytes >= megabyte) && (bytes < gigabyte)) {
        return (bytes / megabyte).toFixed(precision) + ((bps === true) ? " Mb/s" : " MB");

    } else if ((bytes >= gigabyte) && (bytes < terabyte)) {
        return (bytes / gigabyte).toFixed(precision) + ((bps === true) ? " Gb/s" : " GB");

    } else if (bytes >= terabyte) {
        return (bytes / terabyte).toFixed(precision) + ((bps === true) ? " Tb/s" : " TB");

    } else {
        return bytes + ' B';
    }
}

function format_date(timestamp, fmt) {
    var date = new Date(timestamp);
    function pad(value) {
        return (value.toString().length < 2) ? '0' + value : value;
    }
    return fmt.replace(/%([a-zA-Z])/g, function(_, fmtCode) {
        switch (fmtCode) {
            case 'Y':
                return date.getUTCFullYear();
            case 'M':
                return pad(date.getUTCMonth() + 1);
            case 'd':
                return pad(date.getUTCDate());
            case 'H':
                return pad(date.getUTCHours());
            case 'm':
                return pad(date.getUTCMinutes());
            case 's':
                return pad(date.getUTCSeconds());
            default:
                throw new Error('Unsupported format code: ' + fmtCode);
        }
    });
}

function json_syntax_highlight(json) {
    json = json.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
    return json.replace(/("(\\u[a-zA-Z0-9]{4}|\\[^u]|[^\\"])*"(\s*:)?|\b(true|false|null)\b|-?\d+(?:\.\d*)?(?:[eE][+\-]?\d+)?)/g, function(match) {
        var cls = 'number';
        if (/^"/.test(match)) {
            if (/:$/.test(match)) {
                cls = 'key';
            } else {
                cls = 'string';
            }
        } else if (/true|false/.test(match)) {
            cls = 'boolean';
        } else if (/null/.test(match)) {
            cls = 'null';
        }
        return '<span class="' + cls + '">' + match + '</span>';
    });
}