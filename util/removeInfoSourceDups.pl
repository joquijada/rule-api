#!/usr/bin/perl

# Author: Jose Quijada
# Create Date: 20170625
# Description: Written very specifically to remove redundant data wherever
#   possible for info source corroboration. If source A -> source B
#   yields same values as source B -> source A, then it's regarded as duplicate.

use strict;
use warnings;


my %seen;

while(<>) {
    s/[\n\r]+$//;

    my $cur_ln = $_;

    my @flds = split(',', $cur_ln);
    my @sorted_keys = sort(($flds[0], $flds[1]));  
    my $key = join('-', @sorted_keys);
    my $val = join('-', @flds[2..3]);

    if (exists $seen{$key} && $seen{$key} eq $val) {
        print STDERR "JMQ: @flds matches $key, $val, skipping...\n";
        next;
    } elsif (exists $seen{$key}) {
        print STDERR "JMQ: @flds does not match $seen{$key}, not skipping...\n";
    }

    $seen{$key} = $val; 

    my $out =  join(',', @flds);
    print "$out\n";
}
