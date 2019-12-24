#!/usr/bin/perl


use Getopt::Long qw(GetOptions);

use strict;
use warnings;

# Author: Jose Quijada
# Create Date: 20171010
# Description: Find unique DUN's numbers only in a file, and output them
#   to STDOUT (standard output).
#   Give path of file to act on via long option name "--input-file". Can
#   set debug on with --debug flag.


my $in_file;
my $debug;
GetOptions(
  'input-file=s' => \$in_file,
  'debug' => \$debug
) or die "Usage: $0 --input-file <file_path> [--debug]\n";

my %unique;
open(IN_FILE, "$in_file") || die "Could not open $in_file: $!\n";
while(<IN_FILE>) {
    #chop;
    s/[\n\r]+$//;
    my $lin = $_;

    if ($lin !~ /^<waa>.+<gaa>$/i) {
        if ($debug) {
            print STDERR "Skipping record: $lin\n";
        }
        next;
    }
    my ($duns) = ($lin =~ /D(\d+)\*/i);

    if (!$duns) {
        next; 
    }
    if ($debug) {
        print STDERR "Adding: $duns\n";
    }
    ++$unique{$duns};
}
close(IN_FILE);

if ($debug) {
    print "\n\n\n";
}

for my $k (sort keys %unique) {
    if ($unique{$k} > 1) {
        if ($debug) {
            print "DUP: $k, $unique{$k}\n";
        }
        next;
    }
    print "$k\n";
}
