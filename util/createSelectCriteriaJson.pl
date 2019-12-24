#!/usr/bin/perl

# Author: Jose Quijada
# Create Date: 20170728
# Description: Give as lines that contain VERORG selection criteria, and it
#   will generate MIE compliant JSON for adding to left operand table. 
#

use strict;
use warnings;



my $TMPL = qq/{
         "id":"__CNT__",
         "left_operand": "__SELECT_CRIT__",
         "description":""
      },/;

my $cnt = 7;
while(<>) {
    chop();
 
    my $cur_lin = $_;


    if ($cur_lin !~ /VERORG/) {
        next;
    }


    my ($select_crit) = ($cur_lin =~ /(VERORG.+)/);
    my $out = $TMPL;
    $out =~ s/__CNT__/$cnt/;
    $out =~ s/__SELECT_CRIT__/$select_crit/;

    print "$out\n";
    ++$cnt;
}
