#!/usr/bin/perl


use Getopt::Long qw(GetOptions);

use strict;
use warnings;

# Author: Jose Quijada
# Create Date: 20171101
# Description: 
#


my $DEF_START_POS = 0;
my $DEF_PATT_LEN = 9;
my $supplier_file;
my $target_file;
my $start_pos;
my $patt_len; 
my $debug;
GetOptions(
  'grep-pattern-supplier-file=s' => \$supplier_file,
  'grep-target-file=s' => \$target_file,
  'grep-pattern-start-pos=s' => \$start_pos,
  'grep-pattern-len=s' => \$patt_len,
  'debug' => \$debug
) or die "Usage: $0 --input-file <file_path> [--pattern <reg_ex> --debug]\n";

if ($start_pos) {
    $DEF_START_POS = $start_pos; 
}

if ($patt_len) {
    $DEF_PATT_LEN =$patt_len;
}

my %unique;
my %dup_cnt;
open(SUPP_FILE, "$supplier_file") || die "Could not open $supplier_file: $!\n";
while(<SUPP_FILE>) {
    s/[\n\r]+$//;
    my $lin = $_;

    my $patt = substr($lin, $DEF_START_POS, $DEF_PATT_LEN);

 
    if ($debug) {
        print STDERR "Grep'ing $patt from file $target_file\n";
    }

    my $found = `grep $patt $target_file`;

    if ($found) {
        print "$found";
    } else {
        if ($debug) {
            print STDERR "Did not find anything for pattern $patt.\n"
        }
    }
}
close(SUPP_FILE);
