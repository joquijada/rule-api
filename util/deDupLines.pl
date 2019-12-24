#!/usr/bin/perl


use Getopt::Long qw(GetOptions);

use strict;
use warnings;

# Author: Jose Quijada
# Create Date: 20171101
# Description: Collapses duplicate lines based on the specified pattern. 
#


my $REGEX_PATT_FINDER = qr{<DUNSNumber>(.+)<\/DUNSNumber>};
my $in_file;
my $regex_patt_finder_arg;
my $debug;
GetOptions(
  'input-file=s' => \$in_file,
  'pattern=s' => \$regex_patt_finder_arg,
  'debug' => \$debug
) or die "Usage: $0 --input-file <file_path> [--pattern <reg_ex> --debug]\n";

if ($regex_patt_finder_arg) {
    $REGEX_PATT_FINDER = $regex_patt_finder_arg;
}

my %unique;
my %dup_cnt;
open(IN_FILE, "$in_file") || die "Could not open $in_file: $!\n";
while(<IN_FILE>) {
    #chop;
    s/[\n\r]+$//;
    my $lin = $_;

    my ($fld) = ($lin =~ /$REGEX_PATT_FINDER/);

    if (!$fld) {
        next; 
    }
    if ($debug) {
        print STDERR "Adding: $fld\n";
    }
    $unique{$fld} = $lin;
    ++$dup_cnt{$fld}
}
close(IN_FILE);

if ($debug) {
    print "\n\n\n";
}

for my $k (sort keys %unique) {
    if ($debug) {
        print "DUP: $k, $dup_cnt{$k}\n";
    }
    print "$unique{$k}\n";
}
