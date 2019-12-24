#!/usr/bin/perl


use strict;
use warnings;

# Author: Jose Quijada
# Create Date: 20170329
# Description: This script arose from the need to keep InRule from auto-creating
# constraints during import of VERORG XSD's. Currently there's no way
# in InRule Authoring to disable this behavior, as per support ticket
# created here: http://support.inrule.com/cs/forums/t/1095.aspx
# Nothing keeps user from running this script against any other kind of XSD
# as well, as long as the target XSD meets the below criteria:
#     1) The elements which this script target are structure like this
#        <xsd:element...>
#            <xsd:simpleType...>
#                <xsd:restriction base="xsd:<some_type>">
#
#        Each of the elements mentioned above must sit on a line
#        by themselves. 
#
#
# The script will,
#     a) Remove the <xsd:simpleType/> tag entirely.
#     b) Add 'type="xsd:<some_type>"' to the "<xsd:element/>" tag.
#

# Some XSD documents use "xsd" prefix, others "xs". Define
# that prefix in constant below, regardless of what it is
my $XSD_PREFIX = 'xs';
my $rest_found = undef;
my $saved_type = undef;
my @lines = ();
my $tag_to_remove_begin = "<$XSD_PREFIX:simpleType";
my $tag_to_remove_end = "<\/$XSD_PREFIX:simpleType>";

while (<>) {
    #chop();
    s/[\n\r]+$//;
    my $cur_line = $_;
    push @lines, $cur_line;

    print STDERR "Cur line is $cur_line\n";

    # Check if we have encountered a restriction tag. This tag is 
    # inside a "<$XSD_PREFIX:simpleType/>". We'll want to remove that 
    # whole section.
    # In addition, check if this section is eligible for modification. What
    # this is doing is checking for certain element structure to see if
    # it qualifies for removal of restriction tag. See 
    # sectionIsEligibleForRestRemoval() for details on what currently qualifies.
    if ($cur_line =~ /<$XSD_PREFIX:restriction/ 
      && sectionIsEligibleForRestRemoval(\@lines)) {
       $rest_found = 1;

       # Save the type to use it later when reconstructing this section
       # of the XSD
       ($saved_type) = ($cur_line =~ /base="([^"]+)"/); 
    }

    # Continue "scrolling" down the XSD until we encounter the closing
    # "</$XSD_PREFIX:simpleType>", in which case we will remove the entire 
    # "<$XSD_PREFIX:simpleType/>" section.
    if ($cur_line =~ /$tag_to_remove_end/ && $rest_found) {
        my @removed_section = ();
        my $removed_line = "";
        my $removed_lines = "";
        do {
            $removed_line = pop @lines;
            unshift @removed_section, $removed_line; 
            $removed_lines = $removed_line . $removed_lines;  
        } while($removed_line !~ /$tag_to_remove_begin/);

        print STDERR "Removed lines are $removed_lines\n";

        # Ok, entire "<$XSD_PREFIX:simpleType/>" removed, now find 
        # the "<element/>" tag and add the type attr to it. We'll check if 
        # in fact we have an "<element/>" tag, otherwise will exit with error
        # ASSUMPTION: We know we will find an <element/> tag because of 
        #   the "sectionIsEligibleForRestRemoval(\@lines)" check earlier
        $lines[$#lines] =~ /<$XSD_PREFIX:element/ 
          || warn "Was expecting <$XSD_PREFIX:element\/> tag, " 
            . "instead found "
            . "$lines[$#lines] at line ". ($#lines+1) . " of input file!!!"
            . " Will have to scroll up some more to find it." ;
        #print STDERR "Saved type $saved_type\n";
        addTypeAttrToElementTag(\@lines, $saved_type);

        # Now add back the removed section, but with comments around it
        $removed_section[0] = '<!-- JMQ_REMOVED ' .  $removed_section[0];
        $removed_section[$#removed_section] 
          = $removed_section[$#removed_section] . ' -->'; 
        push @lines, @removed_section;

        $rest_found = undef;
    }
}


# Finally, print modified file
for my $ln (@lines) {
    print "$ln\n";
}


sub addTypeAttrToElementTag {
    my ($lines_ref, $type_attr) = @_;
    my $cur_ln_idx = $#$lines_ref;

    while($cur_ln_idx >= 0) {
        my $cur_ln = $lines_ref->[$cur_ln_idx];
        print STDERR "Type Attr Search: $cur_ln, st is $type_attr\n";
        if ($cur_ln =~ /<$XSD_PREFIX:element/) {
            print STDERR "Type Attr Search: $cur_ln, "
              . "st is $type_attr\n";
            $lines_ref->[$cur_ln_idx] =~ s/>/ type="$type_attr"\/>/;

            # Now remove the unncessary </element> closing tag,
            # because above we made <element> self-closing

            $cur_ln = $lines_ref->[$cur_ln_idx];  
            print STDERR "New line is $cur_ln\n";
            return;
        }

        # If true it means we were not inside <XSD_PREFIX:element/> tag
        #if ($cur_ln =~ /<$XSD_PREFIX:element/) {
        #}
        
        --$cur_ln_idx;
    }
}



sub sectionIsEligibleForRestRemoval {
    my $lines_ref = shift @_;
    my $cur_ln_idx = $#$lines_ref;
  
    while($cur_ln_idx >= 0) {
        my $cur_line = $lines_ref->[$cur_ln_idx];
       
        # Check if we're inside an open <element/> container, if not,
        # return false (0)
        if ($cur_line =~ /<\/$XSD_PREFIX:element/i) {
            return 0; 
        }

        if ($cur_line =~ /<$XSD_PREFIX:element/i && $cur_line !~ /type="/) {
            return 1;
        }

        --$cur_ln_idx; 
    } 

    return 0;
}
