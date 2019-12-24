#!/usr/bin/perl

# Author: Jose Quijada
# Create Date: 20170616
# Description: Generates JSON "rows" to be used in MIE entry configs. The
#              field name for the entries is the MIE reserved word "entry".
#              To each row a unique ID gets assigned. 
#              You would plug the output this script generates into a
#              JavaScript JSON variable  that looks like this:
#              var my_json = {table_name: [
#                  <script_output_goes_here>
#                ]};
#              The input file is assumed to have one single entry per line. 
#

use strict;
use warnings;
use Getopt::Long qw(GetOptions);
my $start_id;
GetOptions('start-id=s' => \$start_id) or die "Usage: $0 --start-id <START ID>\n";

my %ents = ();
while(<STDIN>) {
    s/[\n\r]+$//;

    my $ent = $_;

    $ents{$ent}++;
    #my (@ents) = (/(\S+)/g);
    #print STDERR "\@ents is @ents\n";
    #for my $ent (@ents) {
    #    $matched_ents{$ent}++;
    #}
}


my $id = $start_id;
for my $ent (sort keys %ents) {
    my $row = qq/{"id":"$id", "entry":"$ent"},/;
    print "$row\n";
    ++$id;
}
