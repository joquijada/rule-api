#!/usr/bin/perl


use strict;
use warnings;

# Author: Jose Quijada
# Create Date: 20170404
# Description: Normalizes the data used during FB/DM decision in 
#              consumer module of current IE. Main point of this script
#              is to collapse duplicate data that can then be referred to via
#              ID's. The data this script expects as input can be obtained
#              via query below:
#              SELECT PARM_VAL 
#              FROM IEDWM_APPL_SYS_PARM 
#              WHERE PARM_NME = '<GEO_REF_ID>_FB_DM_DECISION_RULES' 
#                AND STAT_INDC = 1
#              If no results, use default:
#              SELECT PARM_VAL 
#              FROM IEDWM_APPL_SYS_PARM 
#              WHERE PARM_NME = 'DFLT_FB_DM_DECISION_RULES' AND STAT_INDC = 1 
#
# Example: 1073_FB_DM_DECISION_RULES||HH|MSR,7,8,9,10|SSR,7,8,9,10~HM|MSR,7,8,9,10|SSR,5,6~HL|MSR,7,8,9,10|SSR,4,3,2,1,0~LH|MSR,4,3,2,1,0|SSR,7,8,9,10~LM|MSR,4,3,2,1,0|SSR,5,6~LL|MSR,4,3,2,1,0|SSR,4,3,2,1,0~MH|MSR,5,6|SSR,7,8,9,10~MM|MSR,5,6|SSR,5,6~ML|MSR,5,6|SSR,4,3,2,1,0
# #
#

my %cc_range;
my $id_cc_range = -1;

my %rec_conf_lvl;
my $id_rec_conf_lvl = -1;
my %rec_conf_lvl_to_cc_range_map;

my %geo;
my $id_geo_to_conf_code_class = 0;
my %geo_to_conf_code_class_map;
my $output_json = 1;


while (<>) {
    #chop();
    s/[\n\r]+$//;
    my $cur_line = $_;

    my @flds = split('\|\|', $cur_line);
    my ($geo_id) = ($flds[0] =~ /^([^_]+)/);

    my @conf_lvl_to_rec_type_to_range_flds = split('~', $flds[1]);

    for my $entry (@conf_lvl_to_rec_type_to_range_flds) {
        my @tokens = split('\|', $entry);
        # HH|MSR,7,8,9,10|SSR,7,8,9,10 
        # $token[0] is the conf lvl combo (HH, HM, LL, etc...,
        # where the first letter is for MSR, the second for SSR
        my ($lvl_msr, $lvl_ssr) = ($tokens[0] =~ /(.)(.)/);  
 
        # $token[1] is the MSR range for first conf lvl letter in $token[0]
        # E.g. "MSR,7,8,9,10"
        storeRecTypeConfLvl($tokens[1], $lvl_msr, $geo_id);
 
        # $token[2] is the SSR range for second conf lvl letter in $token[0]
        # E.g. "SSR,7,8,9,10"
        storeRecTypeConfLvl($tokens[2], $lvl_ssr, $geo_id);
    }
}

printMe();

sub printMe {
    print "Printing ranges table...\n";
    my $json_range = "{\"range_table\": [";
    for my $key (sort { $cc_range{$a} <=> $cc_range{$b} } keys %cc_range) {
        #print "$key ===> $cc_range{$key}\n";
        if ($output_json) {
            $json_range .= qq/{"id": "$cc_range{$key}", "range": [$key]},/;
        } else {
            print "$cc_range{$key},$key\n";
        }
    }
    
    
    if ($output_json) {
        # To get rid of of last extraneous comma (",") added above
        chop($json_range);    
        $json_range .= "]}\n\n"; 
        print "$json_range";
    }


    print "Printing rec type/conf lvl configs...\n";
    my $json_rec_type_conf = "{\"rec_type_conf_lvl_config_table\": [";
    for my $key (sort {$rec_conf_lvl{$a} <=> $rec_conf_lvl{$b}} keys %rec_conf_lvl) {
        #print "$rec_conf_lvl{$key},$key ===> $rec_conf_lvl_to_cc_range_map{$key}\n";
        my ($rec_type, $lvl) = ($key =~ /^([^_]+)__(.+)/);
        if ($output_json) {
            $json_rec_type_conf 
              .= qq/{"id": "$rec_conf_lvl{$key}", "record_type": "$rec_type", "level": "$lvl","range_id": "$rec_conf_lvl_to_cc_range_map{$key}"},/;
        } else {
            print "$rec_conf_lvl{$key},$rec_type,$lvl,$rec_conf_lvl_to_cc_range_map{$key}\n";
        }
    }
    
    if ($output_json) {
        # To get rid of of last extraneous comma (",") added above
        chop($json_rec_type_conf);
        $json_rec_type_conf .= "]}\n\n"; 
        print "$json_rec_type_conf";        
    }


    print "Printing geo configs...\n";
    my $json_geo_conf = "{\"geo_to_conf_lvl_config_table\": [";
    for my $key (keys %geo_to_conf_code_class_map) {
        #print "$id_geo_to_conf_code_class,$key ===> $geo_to_conf_code_class_map{$key}\n";
        my ($geo_ref_id) = ($key =~ /^([^_]+)/);
        if ($output_json) {
            $json_geo_conf .= qq/{"id": "$id_geo_to_conf_code_class", "geo_ref_id": "$geo_ref_id", "rec_type_conf_lvl_config_id": "$geo_to_conf_code_class_map{$key}"},/;
        } else {
            print "$id_geo_to_conf_code_class,$geo_ref_id,$geo_to_conf_code_class_map{$key}\n";
        }
        ++$id_geo_to_conf_code_class;
    }
    
    if ($output_json) {
        # To get rid of of last extraneous comma (",") added above
        chop($json_geo_conf);
        $json_geo_conf .= "]}\n\n"; 
        print "$json_geo_conf";        
    }    
}


sub storeRecTypeConfLvl {
    my ($token, $lvl, $geo_id) = @_;
    my ($rec_type) = ($token =~ /^(.{3})/); 
    my ($range) = ($token =~ /^.{4}(.+)/);
    my $cc_range_id_to_use = storeRange($range);
    my $key = $rec_type . '__' . $lvl;
    $rec_conf_lvl_to_cc_range_map{$key} = $cc_range_id_to_use;

    my $rec_conf_lvl_id_to_use;
    if (not exists $rec_conf_lvl{$key}) {
        ++$id_rec_conf_lvl;
        $rec_conf_lvl_id_to_use = $id_rec_conf_lvl;
        $rec_conf_lvl{$key} = $id_rec_conf_lvl;
    } else {
        $rec_conf_lvl_id_to_use = $rec_conf_lvl{$key};
    }

    $geo_to_conf_code_class_map{$geo_id . '__' . $key}
      = $rec_conf_lvl_id_to_use;
}



sub storeRange {
    my $range = $_[0];
    if (not exists $cc_range{$range}) {
        ++$id_cc_range;
        $cc_range{$range} = $id_cc_range;
        return $id_cc_range;
    } else {
        return $cc_range{$range};
    }
}
