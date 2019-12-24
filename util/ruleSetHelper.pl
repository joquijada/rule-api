#!/usr/bin/perl


use strict;
use warnings;


my $json_tmpl = <<'JSON_TMPL';
    {
      "geo_ref_id":"__GEO_REF_ID__",
      "left_operand":"__LEFT_OP__",
      "info_src_cd":"__INFO_SRC_CD__",
      "operator":"LU||EXACT_MATCH",
      "right_operand":"clusteringElemPrecedenceList-__LIST_NAME__",
      "success_output": "true",
      "failure_output": "false",
      "ignore_element_not_found_error":false
    },
JSON_TMPL


print "{ \"rule_set_name\":[\n";
while(<>) {

    if (!/list_name/) {
        next;
    }

    my $tmpl = $json_tmpl;

    # "list_name":"geoRefId:984-infoSrcCd:20655"
    my ($list_name) = (/"list_name":"(.+)/);
    my ($geo_ref_id) = (/geoRefId:([^-]+)/);
    my ($info_src_cd) = (/infoSrcCd:(\d+)/);

    $tmpl =~ s/__GEO_REF_ID__/$geo_ref_id/;
    $tmpl =~ s/__INFO_SRC_CD__/$info_src_cd/;
    $tmpl =~ s/__LIST_NAME__/$list_name/;

    print "$tmpl";
}

print "]};";
