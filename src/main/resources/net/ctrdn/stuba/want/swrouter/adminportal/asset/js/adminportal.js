var current_view = "system-information";
var current_view_autorefresh = true;

$(document).ready(function() {
    tree_initialize();
    autorefresh_view();

    $(document).on("click", "#save-config-button", function(e) {
        call_swrouter_api("write-startup-configuration", function(data) {
            tree_reload(false);
            reload_view();
        });
        e.stopPropagation();
    });

});

function tree_initialize() {
    $(document).on("click", ".tmlink", function(e) {
        view_name = $(this).attr("_open_view");
        $("#treemenu a span.badge").removeClass("badge");
        $("span", this).addClass("badge");
        load_view(view_name, false);
        e.stopPropagation();
    });

    tree_reload(true);
    load_view(current_view, false);
}

function tree_reload(fullReload) {
    if (fullReload) {
        var tmhtml = "<ul>";
        tmhtml += "<li><span><i class=\"glyphicon glyphicon-cloud\"></i> <span class=\"tmph\" id=\"tmph--hostname\" /></span></span>";
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

        // NAT Submenu
        tmhtml += "<li><span><i class=\"glyphicon glyphicon-retweet\"></i> Network Address Translation</span> <ul>";
        tmhtml += "<li style=\"display: none;\"><a href=\"#\" class=\"tmlink\" _open_view=\"nat-configuration\"><span><i class=\"glyphicon glyphicon-wrench\"></i> Configuration</span></a></li>";
        tmhtml += "<li style=\"display: none;\"><a href=\"#\" class=\"tmlink\" _open_view=\"nat-xlations\"><span><i class=\"glyphicon glyphicon-indent-left\"></i> Active Translations</span></a></li>";
        tmhtml += "</ul></li>";

        // Configuration Management
        tmhtml += "<li><a href=\"#\" class=\"tmlink\" _open_view=\"configuration-management\"><span><i class=\"glyphicon glyphicon-wrench\"></i> Configuration Management</span></a></li>";

        tmhtml += "</ul>";
        tmhtml += "</li>";
        tmhtml += "</ul>";
        $("#treemenu").html(tmhtml);
        $(function() {
            $('.tree li:has(ul)').addClass('parent_li').find(' > span').attr('title', 'Collapse this branch');
            $('.tree li.parent_li > span').on('click', function(e) {
                var children = $(this).parent('li.parent_li').find(' > ul > li');
                if (children.is(":visible")) {
                    children.hide('fast');
                    $(this).attr('title', 'Expand this branch');
                } else {
                    children.show('fast');
                    $(this).attr('title', 'Collapse this branch');
                }
                e.stopPropagation();
            });
        });
    }

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

function load_view(view_name, is_refresh) {
    reset_view();
    data = "";
    switch (view_name) {
        case "system-information":
            {
                data = get_view_system_information(is_refresh);
                current_view_autorefresh = true;
                break;
            }
        case "routing-table":
            {
                data = get_view_routing_table(is_refresh);
                current_view_autorefresh = true;
                break;
            }
        case "routing-static":
            {
                data = get_view_routing_static(is_refresh);
                current_view_autorefresh = false;
                break;
            }
        case "routing-ripv2":
            {
                data = get_view_routing_ripv2(is_refresh);
                current_view_autorefresh = false;
                break;
            }
        case "network-interfaces":
            {
                data = get_view_network_interfaces(is_refresh);
                current_view_autorefresh = true;
                break;
            }
        case "configuration-management":
            {
                data = get_view_configuration_management(is_refresh);
                current_view_autorefresh = false;
                break;
            }
        case "arp":
            {
                data = get_view_arp(is_refresh);
                current_view_autorefresh = true;
                break;
            }
        case "nat-configuration":
            {
                data = get_view_nat_configuration(is_refresh);
                current_view_autorefresh = false;
                break;
            }
        case "nat-xlations":
            {
                data = get_view_nat_xlations(is_refresh);
                current_view_autorefresh = true;
                break;
            }
    }
    if (!is_refresh) {
        current_view = view_name;
        $("#content").html(data);
    }
}

function reload_view() {
    load_view(current_view, true);
}

function autorefresh_view() {
    if (current_view_autorefresh) {
        reload_view();
    }
    setTimeout(function() {
        autorefresh_view();
    }, 1000);
}

function reset_view() {

}













function get_view_system_information(is_refresh) {
    var view_html = "<placeholder identifier=\"system_info\" /><placeholder identifier=\"modules\" /><placeholder identifier=\"pipeline\" />";
    call_swrouter_api("get-system-information", function(data) {
        if (!is_refresh) {
            var html = "<h2><i class=\"glyphicon glyphicon-info-sign\"></i> System Information</h2>";
            html += "<table class=\"table\">";
            html += "<tbody>";
            html += "<tr><td><strong>Hostname</strong></td><td><a href=\"#\" id=\"hostname\">" + data["Response"]["Hostname"] + "</a></td></tr>";
            html += "<tr><td><strong>Uptime</strong></td><td id=\"uptime\">" + data["Response"]["Uptime"] + "ms</td></tr>";
            html += "<tr><td><strong>Boot Date</strong></td><td>" + data["Response"]["BootDate"] + "</td></tr>";
            html += "<tr><td><strong>Boot Time</strong></td><td>" + data["Response"]["BootTime"] + "</td></tr>";
            html += "<tr><td><strong>Interface Count</strong></td><td>" + data["Response"]["InterfaceCount"] + "</td></tr>";
            html += "</tbody>";
            html += "</table>";

            $("#content placeholder[identifier='system_info']").html(html);
        } else {
            $("#content placeholder[identifier='system_info'] td#uptime").html(data["Response"]["Uptime"] + "ms");
        }
        if (!is_refresh) {
            $("#content placeholder[identifier='system_info'] #hostname").editable({
                type: 'text',
                title: 'Enter Hostname',
                placement: "right",
                success: function(response, newValue) {
                    call_swrouter_api_params("set-hostname", "hostname=" + newValue, function(data) {
                        if (data["Response"]["Success"] === true) {
                            tree_reload(false);
                        } else {
                            alert("Failed to change router hostname");
                        }
                    });
                }
            });
        }
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




function get_view_network_interfaces(is_refresh) {
    var view_html = "<placeholder identifier=\"network_interfaces\" />";
    call_swrouter_api("get-network-interfaces", function(data) {
        var html = "<h3><i class=\"glyphicon glyphicon-resize-horizontal\"></i> Network Interfaces</h3>";
        html += "<table class=\"table table-striped\">";
        html += "<thead><tr><th width=\"16\"></th><th>Name</th><th>MTU</th><th>Hardware Address</th><th>IPv4 Address</th><th>RX Packets</th><th>RX Bytes</th><th>TX Packets</th><th>TX Bytes</th><th></th></tr></thead>";
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

            html += "<td>" + obj.ReceivedPacketCount + "</td>";
            html += "<td>" + format_octet_size(obj.ReceivedByteCount) + "</td>";
            html += "<td>" + obj.TransmittedPacketCount + "</td>";
            html += "<td>" + format_octet_size(obj.TransmittedByteCount) + "</td>";

            if (obj["Enabled"]) {
                html += "<td style=\"text-align: right;\"><a href=\"#\" class=\"btn btn-xs btn-danger nic_enabled\" _interface=\"" + obj["Name"] + "\"><i class=\"glyphicon glyphicon-remove\"></i> Disable</a></td>";
            } else {
                html += "<td style=\"text-align: right;\"><a href=\"#\" class=\"btn btn-xs btn-success nic_enabled\" _interface=\"" + obj["Name"] + "\"><i class=\"glyphicon glyphicon-ok\"></i> Enable</a></td>";
            }
            html += "</tr>";
        }
        html += "</tbody>";
        html += "</table>";
        html += "<div class=\"alert alert-warning\" role=\"alert\"><strong>Address removal!</strong> In order to remove interface IPv4 address, set the value to empty or dash.</div>";

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
                    tree_reload(false);
                    reload_view();
                }
            }
        });

        $(".nic_enabled").click(function() {
            call_swrouter_api_params("configure-network-interface", "InterfaceName=" + $(this).attr("_interface") + "&Enabled=toggle", function(data) {
                if (data["UserError"] !== undefined) {
                    alert(data["UserError"]);
                } else {
                    tree_reload(false);
                    reload_view();
                }
            });
        });

    });
    return view_html;
}

function get_view_arp(is_refresh) {
    var view_html = "<h3><i class=\"glyphicon glyphicon-screenshot\"></i> Address Resolution Protocol</h3> <placeholder identifier=\"arp_config\" /> <placeholder identifier=\"arp_table\" /> <placeholder identifier=\"arp_va\" />";

    if (!is_refresh) {
        call_swrouter_api("get-arp-configuration", function(data) {
            var html = "<div class=\"panel panel-default\">"
                    + "<div class=\"panel-heading\">"
                    + "<h3 class=\"panel-title\"><i class=\"glyphicon glyphicon-wrench\"></i> Configuration</h3>"
                    + "</div>"
                    + "<div class=\"panel-body\"><table class=\"table table-striped\">";
            html += "<tr><td width=\"30%\"><strong>Entry Timeout</strong></td><td><a href=\"#\" id=\"arp-entry-timeout\">" + data["Response"]["ARPConfiguration"]["EntryTimeout"] + "ms</a></td></tr>";
            html += "<tr><td><strong>Pipeline Resolution Timeout</strong></td><td><a href=\"#\" id=\"arp-pipeline-resolution-timeout\">" + data["Response"]["ARPConfiguration"]["PipelineResolutionTimeout"] + "ms</a></td></tr>";
            html += "</table></div></div>";

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
                        tree_reload(false);
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
                        tree_reload(false);
                        reload_view();
                    }
                }
            });
        });
    }

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

    call_swrouter_api("get-arp-virtual-addresses", function(data) {
        var html = "<h3><i class=\"glyphicon glyphicon-tag\"></i> Virtual Addresses</h3>";
        html += "<table class=\"table table-striped\">";
        html += "<thead><tr><th width=\"16\"></th><th>IPv4 Address</th><th>Network Interface</th></tr></thead>";
        html += "<tbody>";
        for (var i in data["Response"]["ARPVirtualAddresses"]) {
            var obj = data["Response"]["ARPVirtualAddresses"][i];
            html += "<tr>";
            html += "<td><i class=\"glyphicon glyphicon-tag\"></i></td>";
            html += "<td><strong>" + obj["IPv4Address"] + "</strong></td>";
            html += "<td>" + obj["NetworkInterface"] + "</td>";
            html += "</tr>";
        }
        html += "</tbody>";
        html += "</table>";

        $("#content placeholder[identifier='arp_va']").html(html);
    });

    return view_html;
}


function get_view_routing_table(is_refresh) {
    var view_html = "<placeholder identifier=\"routing_table\" />";

    call_swrouter_api("get-ip-routes", function(data) {
        var html = "<h3><i class=\"glyphicon glyphicon-list\"></i> IP Routing Table</h3>";
        html += "<table class=\"table table-striped\">";
        html += "<thead><tr><th width=\"16\"></th><th width=\"25\">Flags</th><th>Destination</th><th>Gateways</th><th>Administrative Distance</th></tr></thead>";
        html += "<tbody>";
        for (var i in data["Response"]["InstalledRoutes"]) {
            var obj = data["Response"]["InstalledRoutes"][i];
            html += "<tr>";
            if (obj["TargetPrefix"] === "0.0.0.0/0") {
                html += "<td><i class=\"glyphicon glyphicon-globe\"></i></td>";
            } else {
                html += "<td><i class=\"glyphicon glyphicon-share-alt\"></i></td>";
            }
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

function get_view_routing_static(is_refresh) {
    var view_html = "<placeholder identifier=\"static_route_table\" /> <placeholder identifier=\"static_form\" />";

    call_swrouter_api("get-static-ip-routes", function(data) {
        var html = "<h3><i class=\"glyphicon glyphicon-share-alt\"></i> Static Routes</h3>";
        html += "<table class=\"table table-striped\">";
        html += "<thead><tr><th width=\"16\"></th><th>Destination</th><th>Gateways</th><th>Administrative Distance</th><th></th></tr></thead>";
        html += "<tbody>";
        for (var i in data["Response"]["StaticRoutes"]) {
            var obj = data["Response"]["StaticRoutes"][i];
            html += "<tr>";
            if (obj["TargetPrefix"] === "0.0.0.0/0") {
                html += "<td><i class=\"glyphicon glyphicon-globe\"></i></td>";
            } else {
                html += "<td><i class=\"glyphicon glyphicon-share-alt\"></i></td>";
            }
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
            html += "<td style=\"text-align: right;\"><a href=\"#\" data-remove-static-route=\"" + obj["ID"] + "\" class=\"btn btn-xs btn-danger nic_enabled\"><i class=\"glyphicon glyphicon-trash\"></i> Remove</a></td>";
            html += "</tr>";
        }
        html += "</tbody>";
        html += "</table>";
        $("#content placeholder[identifier='static_route_table']").html(html);

        $("#content placeholder[identifier='static_route_table'] a[data-remove-static-route]").click(function() {
            call_swrouter_api_params("remove-static-ip-route", "ID=" + $(this).attr("data-remove-static-route"), function(data) {
                if (data["UserError"] !== undefined) {
                    alert(data["UserError"]);
                } else {
                    tree_reload(false);
                    reload_view();
                }
            });
        });


        html = "";
        html += "<div class=\"panel panel-default\">"
                + "<div class=\"panel-heading\">"
                + "<h3 class=\"panel-title\"><i class=\"glyphicon glyphicon-plus\"></i> Add Static Route</h3>"
                + "</div>"
                + "<div class=\"panel-body\">"
                + "<form class=\"form-vertical\" id=\"add_static_route_form\">"
                + "<div class=\"form-group\">"
                + "<label for=\"in_target\">Target Prefix</label>"
                + "<input type=\"text\" class=\"form-control\" id=\"in_target-prefix\" placeholder=\"CIDR\">"
                + " </div>"
                + "<div class=\"form-group\">"
                + " <label for=\"in_gateways\">Gateways</label>"
                + " <input type=\"email\" class=\"form-control\" id=\"in_gateways\" placeholder=\"Comma-separated gateway list\">"
                + "</div>"
                + "<div class=\"form-group\">"
                + " <label for=\"in_administrative_distance\">Administrative Distance</label>"
                + " <input type=\"email\" class=\"form-control\" id=\"in_administrative_distance\" placeholder=\"AD\" value=\"1\">"
                + "</div>"
                + "<button type=\"submit\" class=\"btn btn-default\">Add Route</button>"
                + "</form>"
                + "</div></div>";
        $("#content placeholder[identifier='static_form']").html(html);

        $("#content placeholder[identifier='static_form'] #add_static_route_form").submit(function() {
            var post_data = "TargetPrefix=" + $("#in_target-prefix").val() + "&Gateways=" + $("#in_gateways").val() + "&AdministrativeDistance=" + $("#in_administrative_distance").val();
            call_swrouter_api_params("add-static-ip-route", post_data, function(data) {
                if (data["UserError"] !== undefined) {
                    alert(data["UserError"]);
                } else {
                    tree_reload(false);
                    reload_view();
                }
            });
            return false;
        });
    });

    return view_html;
}

function get_view_routing_ripv2(is_refresh) {
    var view_html = "<h3><i class=\"glyphicon glyphicon-road\"></i> Routing Information Protocol v2</h3><placeholder identifier=\"ripv2_config\" /><div style=\"width:100%\"><div style=\"width:49%; float:left;\"><placeholder identifier=\"ripv2_interfaces\" /></div><div style=\"width:49%; float:right;\"><placeholder identifier=\"ripv2_networks\" /></div>";

    call_swrouter_api("get-ripv2-configuration", function(data) {
        var html = "<div class=\"panel panel-default\">"
                + "<div class=\"panel-heading\">"
                + "<h3 class=\"panel-title\"><i class=\"glyphicon glyphicon-wrench\"></i> Configuration</h3>"
                + "</div>"
                + "<div class=\"panel-body\"><table class=\"table table-striped\">";
        html += "<tr><td width=\"30%\"><strong>Update Interval</strong></td><td><a href=\"#\" data-ripv2-param=\"UpdateInterval\">" + data["Response"]["RIPv2Configuration"]["UpdateInterval"] + "ms</a></td></tr>";
        html += "<tr><td><strong>Hold Down Timeout</strong></td><td><a href=\"#\" data-ripv2-param=\"HoldDownTimeout\">" + data["Response"]["RIPv2Configuration"]["HoldDownTimeout"] + "ms</a></td></tr>";
        html += "<tr><td><strong>Flush Timeout</strong></td><td><a href=\"#\" data-ripv2-param=\"FlushTimeout\">" + data["Response"]["RIPv2Configuration"]["FlushTimeout"] + "ms</a></td></tr>";
        html += "</table></div></div>";

        $("#content placeholder[identifier='ripv2_config']").html(html);

        $("#content placeholder[identifier='ripv2_config'] a[data-ripv2-param]").editable({
            type: "text",
            placement: "right",
            url: function(params) {
                var d = new $.Deferred;
                call_swrouter_api_params("configure-ripv2", $(this).attr("data-ripv2-param") + "=" + params.value, function(data) {
                    d.resolve(data);
                });
                return d.promise();
            },
            success: function(response, newValue) {
                if (response["UserError"] !== undefined) {
                    return response["UserError"];
                } else {
                    tree_reload(false);
                    reload_view();
                }
            }
        });
    });

    call_swrouter_api("get-ripv2-interfaces", function(data) {
        var html = "<div class=\"panel panel-default\">"
                + "<div class=\"panel-heading\">"
                + "<h3 class=\"panel-title\"><i class=\"glyphicon glyphicon-resize-horizontal\"></i> Interfaces</h3>"
                + "</div>"
                + "<div class=\"panel-body\"><table class=\"table table-striped\">";
        for (var i in data["Response"]["RIPv2Interfaces"]) {
            var iface = data["Response"]["RIPv2Interfaces"][i];
            html += "<tr><td><strong>" + iface["Name"] + "</strong></td>";
            if (iface["Enabled"]) {
                html += "<td style=\"text-align: right;\"><a href=\"#\" class=\"btn btn-xs btn-danger\" data-ripv2-iface-toggle=\"" + iface["Name"] + "\"><i class=\"glyphicon glyphicon-remove\"></i> Disable</a></td>";
            } else {
                html += "<td style=\"text-align: right;\"><a href=\"#\" class=\"btn btn-xs btn-success\" data-ripv2-iface-toggle=\"" + iface["Name"] + "\"><i class=\"glyphicon glyphicon-ok\"></i> Enable</a></td>";
            }
            html += "</tr>";
        }
        html += "</table></div></div>";

        $("#content placeholder[identifier='ripv2_interfaces']").html(html);

        $("#content placeholder[identifier='ripv2_interfaces'] a[data-ripv2-iface-toggle]").click(function() {
            call_swrouter_api_params("configure-ripv2-interface", "InterfaceName=" + $(this).attr("data-ripv2-iface-toggle") + "&Enabled=toggle", function(data) {
                if (data["UserError"] !== undefined) {
                    alert(data["UserError"]);
                } else {
                    tree_reload(false);
                    reload_view();
                }
            });
        });
    });


    call_swrouter_api("get-ripv2-networks", function(data) {
        var html = "<div class=\"panel panel-default\">"
                + "<div class=\"panel-heading\">"
                + "<h3 class=\"panel-title\"><i class=\"glyphicon glyphicon-link\"></i> Networks</h3>"
                + "</div>"
                + "<div class=\"panel-body\"><table class=\"table table-striped\">";
        for (var i in data["Response"]["RIPv2Networks"]) {
            var network = data["Response"]["RIPv2Networks"][i];
            html += "<tr><td><strong>" + network + "</strong></td>";
            html += "<td style=\"text-align: right;\"><a href=\"#\" class=\"btn btn-xs btn-danger\" data-ripv2-network-delete=\"" + network + "\"><i class=\"glyphicon glyphicon-trash\"></i> Remove</a></td>";
            html += "</tr>";
        }
        html += "</table>"
                + "<form class=\"form-inline\" id=\"add_ripv2_network_form\">"
                + "<div class=\"form-group\">"
                + "<label for=\"in_network_prefix\">Prefix</label>"
                + "<input type=\"text\" class=\"form-control\" id=\"in_network_prefix\" placeholder=\"CIDR\">"
                + " </div>"
                + "<button type=\"submit\" class=\"btn btn-default\">Add Network</button>"
                + "</form>"
                + "</div></div>";

        $("#content placeholder[identifier='ripv2_networks']").html(html);

        $("#content placeholder[identifier='ripv2_networks'] a[data-ripv2-network-delete]").click(function() {
            call_swrouter_api_params("remove-ripv2-network", "IPv4Prefix=" + $(this).attr("data-ripv2-network-delete"), function(data) {
                if (data["UserError"] !== undefined) {
                    alert(data["UserError"]);
                } else {
                    tree_reload(false);
                    reload_view();
                }
            });
        });

        $("#content placeholder[identifier='ripv2_networks'] form#add_ripv2_network_form").submit(function() {
            call_swrouter_api_params("add-ripv2-network", "IPv4Prefix=" + $("#in_network_prefix").val(), function(data) {
                if (data["UserError"] !== undefined) {
                    alert(data["UserError"]);
                } else {
                    tree_reload(false);
                    reload_view();
                }
            });
        });
    });

    return view_html;
}

function get_view_nat_configuration(is_refresh) {
    var view_html = "<h3><i class=\"glyphicon glyphicon-random\"></i> NAT Configuration</h3> <placeholder identifier=\"nat_configuration\" /> <placeholder identifier=\"nat_pools\" /> <placeholder identifier=\"nat_rules\" />";

    call_swrouter_api("get-nat-configuration", function(data) {
        var html = "<table class=\"table table-striped\">";
        html += "<tr><td width=\"30%\"><strong>Address Translation Timeout</strong></td><td><a href=\"#\" data-nat-param=\"AddressTranslationTimeout\">" + data["Response"]["NATConfiguration"]["AddressTranslationTimeout"] + "ms</a></td></tr>";
        html += "<tr><td><strong>Port Translation Timeout</strong></td><td><a href=\"#\" data-nat-param=\"PortTranslationTimeout\">" + data["Response"]["NATConfiguration"]["PortTranslationTimeout"] + "ms</a></td></tr>";
        html += "<tr><td><strong>Translation Hold Down Timeout</strong></td><td><a href=\"#\" data-nat-param=\"TranslationHoldDownTimeout\">" + data["Response"]["NATConfiguration"]["TranslationHoldDownTimeout"] + "ms</a></td></tr>";
        html += "</table>";

        $("#content placeholder[identifier='nat_configuration']").html(html);

        $("a[data-nat-param]").editable({
            type: "text",
            placement: "right",
            url: function(params) {
                var d = new $.Deferred;
                call_swrouter_api_params("configure-nat", $(this).attr("data-nat-param") + "=" + params.value, function(data) {
                    d.resolve(data);
                });
                return d.promise();
            },
            success: function(response, newValue) {
                if (response["UserError"] !== undefined) {
                    return response["UserError"];
                } else {
                    tree_reload(false);
                    reload_view();
                }
            }
        });
    });

    call_swrouter_api("get-nat-pools", function(data) {
        var html = "<div class=\"panel panel-default\">"
                + "<div class=\"panel-heading\">"
                + "<h3 class=\"panel-title\"><i class=\"glyphicon glyphicon-th\"></i> Pools</h3>"
                + "</div>"
                + "<div class=\"panel-body\"><table class=\"table table-striped\">"
                + "<thead><tr><th width=\"16\"></th><th>Name</th><th>Prefix</th><th>Addresses</th><th></th></tr></thead>";
        for (var i in data["Response"]["NATPools"]) {
            var pool = data["Response"]["NATPools"][i];
            html += "<tr>";
            html += "<td><i class=\"glyphicon glyphicon-th-large\"></i></td>";
            html += "<td><strong>" + pool.Name + "</strong></td>";
            html += "<td>" + pool.Prefix + "</td>";
            if (pool.Addresses.length === 0) {
                html += "<td><i><a href=\"#\" data-nat-pool-edit-addresses=\"" + pool.ID + "\">none configured</a></i></td>";
            } else {
                var addrString = "";
                for (var j in pool.Addresses) {
                    if (addrString !== "") {
                        addrString += "\n";
                    }
                    addrString += pool.Addresses[j];
                }
                html += "<td><a href=\"#\" data-nat-pool-edit-addresses=\"" + pool.ID + "\">" + addrString + "</a></td>";
            }
            html += "<td style=\"text-align: right;\"><a href=\"#\" class=\"btn btn-xs btn-danger\" data-nat-pool-delete=\"" + pool.ID + "\"><i class=\"glyphicon glyphicon-trash\"></i> Remove</a></td>";
        }
        html += "</table>";
        html += "</tr>"
                + "<form class=\"form-vertical\" id=\"add_nat_pool\">"
                + "<div class=\"form-group\">"
                + "<label for=\"in_pool-name\">Name</label>"
                + "<input type=\"text\" class=\"form-control\" id=\"in_pool-name\" placeholder=\"NAT Pool\">"
                + " </div>"
                + "<div class=\"form-group\">"
                + " <label for=\"in_pool-prefix\">Prefix</label>"
                + " <input type=\"email\" class=\"form-control\" id=\"in_pool-prefix\" placeholder=\"CIDR\">"
                + "</div>"
                + "<button type=\"submit\" class=\"btn btn-default\">Add Pool</button>"
                + "</form></div></div>";


        $("#content placeholder[identifier='nat_pools']").html(html);

        $("#content placeholder[identifier='nat_pools'] a[data-nat-pool-delete]").click(function() {
            call_swrouter_api_params("remove-nat-pool", "ID=" + $(this).attr("data-nat-pool-delete"), function(data) {
                if (data["UserError"] !== undefined) {
                    alert(data["UserError"]);
                } else {
                    tree_reload(false);
                    reload_view();
                }
            });
        });

        $("#content placeholder[identifier='nat_pools'] form#add_nat_pool").submit(function() {
            var params = "Name=" + $("#in_pool-name").val() + "&Prefix=" + $("#in_pool-prefix").val();
            call_swrouter_api_params("add-nat-pool", params, function(data) {
                if (data["UserError"] !== undefined) {
                    alert(data["UserError"]);
                } else {
                    tree_reload(false);
                    reload_view();
                }
            });
            return false;
        });

        $("#content placeholder[identifier='nat_pools'] a[data-nat-pool-edit-addresses]").editable({
            type: "textarea",
            placement: "right",
            escape: true,
            url: function(params) {
                var d = new $.Deferred;
                call_swrouter_api_params("configure-nat-pool-addresses", "ID=" + $(this).attr("data-nat-pool-edit-addresses") + "&Addresses=" + params.value, function(data) {
                    d.resolve(data);
                });
                return d.promise();
            },
            success: function(response, newValue) {
                if (response["UserError"] !== undefined) {
                    return response["UserError"];
                } else {
                    tree_reload(false);
                    reload_view();
                }
            }
        });
    });

    call_swrouter_api("get-nat-rules", function(data) {
        var html = "<div class=\"panel panel-default\">"
                + "<div class=\"panel-heading\">"
                + "<h3 class=\"panel-title\"><i class=\"glyphicon glyphicon-list\"></i> Rules</h3>"
                + "</div>"
                + "<div class=\"panel-body\"><table class=\"table table-striped\">"
                + "<thead><tr><th width=\"16\"></th><th>Priority</th><th>Summary</th><th>ECMP Outside Interfaces</th><th></th></tr></thead>";
        for (var i in data["Response"]["NATRules"]) {
            var rule = data["Response"]["NATRules"][i];
            html += "<tr>";
            html += "<td><i class=\"glyphicon glyphicon-list\"></i></td>";
            html += "<td><a href=\"#\" data-nat-rule-edit=\"" + rule.ID + "\" data-nat-rule-param=\"Priority\">" + rule.Priority + "</a></td>";
            html += "<td style=\"font-size: 10px;\"><strong>" + $("<strong />").text(rule.Summary).html() + "</strong></td>";
            if (rule.Configuration.ECMPOutsideInterfaces === null || rule.Configuration.ECMPOutsideInterfaces.length === 0) {
                html += "<td><i><a href=\"#\" data-nat-rule-edit=\"" + rule.ID + "\" data-nat-rule-param=\"ECMPOutsideInterfaces\">none</a></i></td>";
            } else {
                var ecmpoiString = "";
                for (var j in rule.Configuration.ECMPOutsideInterfaces) {
                    if (ecmpoiString !== "") {
                        ecmpoiString += "\n";
                    }
                    ecmpoiString += rule.Configuration.ECMPOutsideInterfaces[j];
                }
                html += "<td><a href=\"#\" data-nat-rule-edit=\"" + rule.ID + "\" data-nat-rule-param=\"ECMPOutsideInterfaces\">" + ecmpoiString + "</a></td>";
            }
            html += "<td style=\"text-align: right;\"><a href=\"#\" class=\"btn btn-xs btn-danger\" data-nat-rule-delete=\"" + rule.ID + "\"><i class=\"glyphicon glyphicon-trash\"></i> Remove</a></td>";
        }
        html += "</table>";
        html += "</tr>"
                + "<form class=\"form-vertical\" id=\"add_nat_rule_form\">"
                + "<div class=\"form-group\">"
                + " <label for=\"in_rule_type\">Type</label>"
                + " <select id=\"in_rule_type\" data-submit-field-name=\"Type\"><option value=\"SNAT_INTERFACE\">SNAT_INTERFACE</option><option value=\"SNAT_POOL\">SNAT_POOL</option><option value=\"DNAT\">DNAT</option></select>"
                + "</div>"
                + "<div class=\"form-group\">"
                + "<label for=\"in_rule_priority\">Priority</label>"
                + "<input type=\"text\" class=\"form-control\" id=\"in_rule_priority\"  data-submit-field-name=\"Priority\" placeholder=\"1000\" value=\"1000\">"
                + " </div>"
                + "<div class=\"form-group\">"
                + " <label for=\"in_rule_inside_address\">Inside Address</label>"
                + " <input type=\"email\" class=\"form-control\" id=\"in_rule_inside_address\"  data-submit-field-name=\"InsideAddress\" placeholder=\"IPv4 Address\">"
                + "</div>"
                + "<div class=\"form-group\">"
                + " <label for=\"in_rule_outside_address\">Outside Address</label>"
                + " <input type=\"email\" class=\"form-control\" id=\"in_rule_outside_address\"  data-submit-field-name=\"OutsideAddress\" placeholder=\"IPv4 Address\">"
                + "</div>"
                + "<div class=\"form-group\">"
                + " <label for=\"in_rule_inside_prefix\">Inside Prefix</label>"
                + " <input type=\"email\" class=\"form-control\" id=\"in_rule_inside_prefix\"  data-submit-field-name=\"InsidePrefix\" placeholder=\"IPv4 CIDR\">"
                + "</div>"
                + "<div class=\"form-group\">"
                + " <label for=\"in_rule_outside_interface\">Outside Interface</label>"
                + " <select id=\"in_rule_outside_interface\"  data-submit-field-name=\"OutsideInterface\"></select>"
                + "</div>"
                + "<div class=\"form-group\">"
                + " <label for=\"in_rule_outside_pool\">Outside Pool</label>"
                + " <select id=\"in_rule_outside_pool\"  data-submit-field-name=\"OutsidePool\"></select>"
                + "</div>"
                + "<div class=\"form-group\">"
                + " <label for=\"in_rule_overload_enabled\">Overload Enabled</label>"
                + " <select id=\"in_rule_overload_enabled\"  data-submit-field-name=\"OverloadEnabled\"><option value=\"true\">Yes</option><option value=\"false\">No</option></select>"
                + "</div>"
                + "<div class=\"form-group\">"
                + " <label for=\"in_rule_protocol\">Protocol</label>"
                + " <select id=\"in_rule_protocol\"  data-submit-field-name=\"Protocol\"><option value=\"\">Any</option><option value=\"TCP\">TCP</option><option value=\"UDP\">UDP</option><option value=\"ICMP\">ICMP</option></select>"
                + "</div>"
                + "<div class=\"form-group\">"
                + " <label for=\"in_rule_inside_ps_identifier\">Inside Port or ICMP Identifier</label>"
                + " <input type=\"email\" class=\"form-control\" id=\"in_rule_inside_ps_identifier\"  data-submit-field-name=\"InsideProtocolSpecificIdentifier\" placeholder=\"Port or Identifier\">"
                + "</div>"
                + "<div class=\"form-group\">"
                + " <label for=\"in_rule_outside_ps_identifier\">Outside Port or ICMP Identifier</label>"
                + " <input type=\"email\" class=\"form-control\" id=\"in_rule_outside_ps_identifier\"  data-submit-field-name=\"OutsideProtocolSpecificIdentifier\" placeholder=\"Port or Identifier\">"
                + "</div>"
                + "<button type=\"submit\" class=\"btn btn-default\">Add Rule</button>"
                + "</form></div></div>";


        $("#content placeholder[identifier='nat_rules']").html(html);

        call_swrouter_api("get-network-interfaces", function(data) {
            var options_html = "";
            for (var i in data.Response.NetworkInterfaces) {
                var iface = data.Response.NetworkInterfaces[i];
                options_html += "<option value=\"" + iface.Name + "\">" + iface.Name + "</option>";
            }
            $("#in_rule_outside_interface").html(options_html);
        });

        call_swrouter_api("get-nat-pools", function(data) {
            var options_html = "";
            for (var i in data.Response.NATPools) {
                var pool = data.Response.NATPools[i];
                options_html += "<option value=\"" + pool.Name + "\">" + pool.Name + "</option>";
            }
            $("#in_rule_outside_pool").html(options_html);
        });

        $("select[data-submit-field-name='Type']").change(function() {
            var fs = $("form#add_nat_rule_form");
            $("input, select", fs).parent("div").hide();
            $("#in_rule_type, #in_rule_priority", fs).parent("div").show();
            switch ($(this).val()) {
                case "SNAT_INTERFACE":
                    {
                        $("#in_rule_inside_prefix, #in_rule_outside_interface", fs).parent("div").show();
                        break;
                    }
                case "SNAT_POOL":
                    {
                        $("#in_rule_inside_prefix, #in_rule_outside_pool, #in_rule_overload_enabled", fs).parent("div").show();
                        break;
                    }
                case "DNAT":
                    {
                        $("#in_rule_inside_address, #in_rule_outside_address, #in_rule_protocol, #in_rule_inside_ps_identifier, #in_rule_outside_ps_identifier", fs).parent("div").show();
                        break;
                    }
            }
        }).change();

        $("select[data-submit-field-name='Protocol']").change(function() {
            var fs = $("form#add_nat_rule_form");
            switch ($(this).val())
            {
                case "TCP":
                case "UDP" :
                    {
                        $("#in_rule_inside_ps_identifier, #in_rule_outside_ps_identifier", fs).parent("div").show();
                        break;
                    }
                case "ICMP":
                default :
                    {
                        $("#in_rule_inside_ps_identifier, #in_rule_outside_ps_identifier", fs).parent("div").hide();
                        break;
                    }
            }
        }).change();

        $("form#add_nat_rule_form").submit(function() {
            var post_data = "";
            $("[data-submit-field-name]", $(this)).each(function() {
                if (post_data !== "") {
                    post_data += "&";
                }
                post_data += $(this).attr("data-submit-field-name") + "=" + $(this).val();
            });
            call_swrouter_api_params("add-nat-rule", post_data, function(data) {
                if (data["UserError"] !== undefined) {
                    alert(data["UserError"]);
                } else {
                    tree_reload(false);
                    reload_view();
                }
            });
            return false;
        });

        $("#content placeholder[identifier='nat_rules'] a[data-nat-rule-delete]").click(function() {
            call_swrouter_api_params("remove-nat-rule", "ID=" + $(this).attr("data-nat-rule-delete"), function(data) {
                if (data["UserError"] !== undefined) {
                    alert(data["UserError"]);
                } else {
                    tree_reload(false);
                    reload_view();
                }
            });
        });

        $("#content placeholder[identifier='nat_rules'] a[data-nat-rule-edit]").editable({
            type: "textarea",
            placement: "right",
            escape: true,
            url: function(params) {
                var d = new $.Deferred;
                call_swrouter_api_params("configure-nat-rule", "ID=" + $(this).attr("data-nat-rule-edit") + "&" + $(this).attr("data-nat-rule-param") + "=" + params.value, function(data) {
                    d.resolve(data);
                });
                return d.promise();
            },
            success: function(response, newValue) {
                if (response["UserError"] !== undefined) {
                    return response["UserError"];
                } else {
                    tree_reload(false);
                    reload_view();
                }
            }
        });
    });
    return view_html;
}

function get_view_nat_xlations(is_refresh) {
    var view_html = "<h3><i class=\"glyphicon glyphicon-indent-left\"></i> Active NAT Translations</h3> <placeholder identifier=\"nat_xlations\" />";

    call_swrouter_api("get-nat-translations", function(data) {
        var html = "<table class=\"table table-striped\">";
        html += "<thead><tr><th width=\"16\"></th><th>Summary</th><th>Last Activity</th><th>Timeout</th><th>Timeout in</th><th>Active</th><th>XLATE Hits</th><th>UNXLATE Hits</th></tr></thead>";
        for (var i in data["Response"]["NATTranslations"]) {
            var xlation = data["Response"]["NATTranslations"][i];
            html += "<tr>";
            html += "<td style=\"text-align: center;\"><i class=\"glyphicon glyphicon-" + ((xlation.Type === "PAT") ? "tag" : "tags") + "\"></i></td>";
            html += "<td style=\"font-size: 10px;\"><strong>" + $("<strong />").text(xlation.Summary).html() + "</strong></td>";
            html += "<td>" + xlation.LastActivityDate + "</td>";
            html += "<td>" + xlation.Timeout + "ms</td>";
            html += "<td>" + ((xlation.TimeRemaining > 0) ? xlation.TimeRemaining + "ms" : "<i>timed out</i>") + "</td>";
            html += "<td style=\"text-align: center\"><i class=\"glyphicon glyphicon-" + ((xlation.Active) ? "ok text-success" : "remove text-danger") + "\"></i></td>";
            html += "<td>" + xlation.TranslateHitCount + "</td>";
            html += "<td>" + xlation.UntranslateHitCount + "</td>";
            html += "</tr>";
        }
        html += "</table>";

        $("#content placeholder[identifier='nat_xlations']").html(html);
    });

    return view_html;
}










function get_view_configuration_management(is_refresh) {
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