#!/usr/bin/perl


use strict;
use warnings;

use Getopt::Long qw(GetOptions);
# TODO: Collapse mapper rows even further by finding dup list of list ID's, 
#   and in a single row associate those to the list of entries they refer to.
# TODO: 
# Author: Jose Quijada
# Create Date: 20170625
# Description:
#   This script does all the hardword of generating JSON for lists of 
#   entries that are categorized according to one or more attributes. The
#   script can generate lists for as many attribute value combinations as
#   needed. 
#
#   In mathematical terms, think of intersection of sets, where a set
#   is defined by each indivual attribute (aka category) value. For example,
#   if we're talking about ice cream brand (the entries) and available flavors,
#   one set contains chocolate, another vanilla, and so on. If we have a second
#   attribute we want to use to classify the various ice cream brands, 
#   say color, we can have sets red, white, green, etc... Now, if
#   you want all ice creams which have flavor and color in common, that
#   produces other sets which are intersections of the original sets that look
#   at each individual value alone, and this is is exactly what this script
#   produces: sets for each individual attribute value, and/or sets
#   that produce intersections based on any combination of 
#   attributes and their values.
#   Extending the example above, if you have requested this script to
#   produce ice cream brands that are both flavor strawberry and color white,
#   that right there is a set produced by intersecting on flavor and color. 
#   Likewise additional sets are created for all unique combination of 
#   flavor/color values found in the input data. If the input data contained 
#   just one ice cream which flavor was chocolocate and color is blue, then 
#   that's another set produced, with just one entry in this case. But if 
#   another ice cram brand happened to also sell ce cream which flavor 
#   is chocolate and color is blue, then the set will contain two entries.
#
#   There will be a list generated for *every* unique attribute value
#   combination encountered in the row data input.
#   For example, if the entries in questions can be categorized according
#   to three attributes "attr1", "attr2" and "attr3", and it is requested 
#   to generate a list for every unique combination of these 3 attributes, 
#   the produced lists would look like:
#     attr1<X>atrr2<Y>attr3<Z>
#
#   where X, Y, Z are the unique values encountered in the data set for
#   the corresponding attribute. Notice how the number of lists produced
#   equals the number of unique combinations of X, Y and Z.
#
#   To make it concrete, and using a very simple example, in current IE 
#   for instance we have info source codes which are categorized according 
#   to geo ref id *and* according to FB *and* DM eligiblity. If you 
#   requested two combinations, one is geo ref id + FB eligibility, and 
#   another combination is geo ref id + DM eligiblity, then these are lists 
#   produced, assuming the input data set contained only info src code 
#   "12345", and that it had 4 rows: 2 each for geo ref id's 1073 and 892, 
#   and for both geo ref id's FB = 1 and DM = 1:
#
#   geoRefId1073fbInd1 - info source code 12345
#   geoRefId892fbInd1  - info source code 12345
#   geoRefId1073dmInd1 - info source code 12345
#   geoRefId892dmInd1  - info source code 12345
#
#   Expected arguments are: 
#     --list-name-tmpl - A comma-separated list of list "templates" to build
#       the actual list names. This argument value must be passed using the
#       below format:
#       'fielTmpl1-1,fieldTmpl2-2,...fieldTmplN-N'
#
#       The "fielTmpl" part is what will appear in the list name, followed by
#       the value found in the input rows for such field. The digit
#       after the dash (-) corresponds to the field position (0-based) in the 
#       input rows that the template should bind to.
#       This helps generates "buckets", if you will, for each unique 
#       combination of field values 
#       found in the input data. The "--attr-combos" is
#       then used to tell the script the bucket combinations to use to 
#       generate lists.
#       At least for now, Use dummy/filler template values for fields which 
#       you're not interested in including in the final results. This is a 
#       temporary workaround until a permanent solution is implemented. For 
#       example:
#         --list-name-tmpl 'dummy:0,dummy:1,fldA:2,dummy:3,fldB:4'
#       The above is effectively saying that only field index 2 and 4 are
#       of interest, therefore for other field indeces, just use a dummy field
#       template. This then implies that you specified --attr-combos value as
#       '2,4'.
#       Update 20170915 - the filler stuff no longer necessary after list name
#         input style change, as long all indeces passed --attr-combos
#         are accounted for in --list-name-tmpl argument.
#     --input-file - The path (can be absolute or relative) of a file that 
#        has a list of comma separated records. The *last* field in each row 
#        *must* be the entry in question. But if you want to designate 
#        another field(s) as the entry, then see '--entry-flds'. 
#     --rule-group-prefix - Group name. This value will be the prefix used for
#       the name of the various JSON structures produced by this script
#     --attr-combos - Groupings requested. This is done by passing a pipe 
#       (|) separated list of comma-separated indeces that correspond to the 
#       attribute groupings you're interested in, for example: '0,1|0,2,|0,3'
#     --single-attr-lists - This is just a flag which when present will cause 
#       the script to generate "solo" (as in Han Solo from Star Wars :-)) 
#       attribute lists, meaning an individual list for each single attribute 
#       value, not combined with any other attributes.
#     --additional-entry-fields - By default only the last field of the input
#       data gets included in the entry JSON/table. If you want to include
#       more fields, use this argument, and format the value as:
#       'fld_hd1r=<POS>,fld_hdr2=<POS>'
#       <POS> is the 0-based position in input file of the field you want 
#       to add. The field name that will appear in the entry JSON is the
#       "fld_hdrX" value
#     --entry-flds Give here a comma separated list of the fields that will 
#       form the lookup entry. You can specify one or more fields. By default
#       the separator used will be a single space. To override that, add the 
#       desired delimiter by adding a pipe at the end of the list of indeces,
#       separated by a single pipe. Example:
#         --entry-flds '0,4,6|-'
#       If you don't specify this argument, the default behavior is to
#       use the last field in the input file as the entry.
#     --entry-start-id Allows caller to specify the entry universe beginning
#       index to use. Default is 0. This option exists to support adding
#       more entries to an already existing entry universe
#     --list-start-id Same as --entry-start-id but for the ID of the lists
#     --existing-universe-json-rows - The name of file which contains existing
#       universe, if any. This is useful when you want to add entries after the
#       fact. If an entry already exists in new input file, duplicates will
#       not get added under this scheme. Assignment of ID's to entry picks up
#       from the highest ID found in the existing entries, therefore you
#       need not worry that the ID's of your existing entries will change. This
#       helps when entries have been added manually after initial creation,
#       and those entries have already been associated with lists via
#       mappers.
#     --sort-entry-vals TODO
#
#
#   Failure to follow any of the requirements above will result in 
#   incorrect data.
#
#   Example:
#     ./createListConfigurations.pl --list-name-tmpl geoRefId:0,cc:1,infoSrcCd:2,dmInd:3,fbInd:4' --input-file <path_to_file_containing_config_records> --rule-group-prefix <group_name> --attr-combos <list_of_requested_groupings> --single-attr-lists
#
#   The script will generate MIE configuration compliant JSON. The JSON
#   generated are:
#     1) The list of unique target entries, with an ID per row
#     2) The list of configuration lists, with an ID per row
#     3) For every config list, a "mapper" JSON that maps the list to its
#        target entries 
#

my $DEBUG = 'DEBUG:';

my $list_name_tmpl_str;
my $lists_file;
my $group_name;
my $combo_list_flds;
my $gen_indiv_lists;
my $add_entry_flds_str;
my $entry_flds;
my $entry_start_id;
my $list_start_id;
my $existing_univ;
my $sort_entry_vals;
GetOptions('list-name-tmpl=s' => \$list_name_tmpl_str,
  'input-file=s' => \$lists_file,
  'rule-group-prefix=s' => \$group_name,
  'attr-combos=s' => \$combo_list_flds,
  'single-attr-lists' => \$gen_indiv_lists,
  'additional-entry-fields=s' => \$add_entry_flds_str,
  'entry-flds=s' => \$entry_flds,
  'entry-start-id=s' => \$entry_start_id,
  'list-start-id=s' => \$list_start_id,
  'existing-universe-json-rows=s' => \$existing_univ,
  'sort-entry-vals' => \$sort_entry_vals
) or die "Usage: $0 --list-name-tmpl 'fldTmplA:0, fldTmplB:1,...' --input-file <path to a file> --attr-combos '0,1|3,4,5|...' (Optional args) --single-attr-lists> --additional-entry-fields '0,1,2,...' --entry-flds '0,4,6|-'\n";

# 
# Do input validation
#
if ($entry_start_id && $existing_univ) {
    die "Cannot specify both --existing-universe-json-rows and"
      . " --entry-start-id. Pick one or the other, then try again. "
      . "The reason for this is that, when you specify an existing  "
      . "universe, the program will automatically calculate the starting "
      . "entry ID, based on he highest ID found in the existing entries.\n";
}


my %list_tmpls = %{buildHashMapFromString(',', '-', $list_name_tmpl_str)};

my $TAB_TAG_OPEN =  qq/{"<TABLE_NAME>": [\n/;
my $TAB_TAG_CLOSE = qq/]}\n/;

# Below is the list name that applies to all combos 
# irrespective of the attributes in the IE. In other words, it's
# a list that applies to all IE data that comes into MIE
my $ALL_COMBOS_LIST_NAME = 'allCombos';

#
# Map list entries to unique ID's. Remember the last field is
# the entry, as long as the user followed instructions.
# Also we capture any additional fields which should be added in the
# entry data source. The map is built now and used later on when
# printing out the entry JSON
#
my $ENTRY_ID = 0;
if ($entry_start_id) {
    $ENTRY_ID = $entry_start_id
}
my %entry_to_id;
my %id_to_entry;
my %id_to_add_ent_flds;

# Parse passed in additional fields to add to entry "table", by turning
# into a list the position of additional fields which should get included
my @add_entry_flds;
if ($add_entry_flds_str) {
    @add_entry_flds = 
      @{turnDelimSeparatedStringToList(',', $add_entry_flds_str)};
}


#
# Logic to figure out what the entry fields should be,
# and the delimiter to use
my @entry_fld_idx = ();
my $entry_delim = ' ';
if ($entry_flds !~ /^$/) {
    my @tmp = @{turnDelimSeparatedStringToList('\|', $entry_flds)};
    @entry_fld_idx = @{turnDelimSeparatedStringToList(',', $tmp[0])};
    if (@tmp > 1) {
        $entry_delim = $tmp[1];
    }
}

if (@entry_fld_idx > 0) {
    print STDERR "$DEBUG Entry field idx' is are @entry_fld_idx\n";
}


if ($existing_univ) {
    addExistingEntries($existing_univ);
}

#
# First generate the entry universe. Use existing entry file if one was
# specified, and then the new entries data file. From the input file 
# generate the ID-to-entry hash
open(IN, "$lists_file") || die "Coult not open file $lists_file: $!";
while(<IN>) {
    s/[\n\r]+$//;
    my @flds = split(',');
  
    # Calculate the entry for the row currently
    # being processed. 
    my $entry = entry(\@flds, $entry_delim); 


    print STDERR "$DEBUG \$entry is $entry\n";

    # Skip if entry already existed. Below returns false
    # in such cases 
    if (not addEntry($ENTRY_ID, $entry, \%entry_to_id, \%id_to_entry)) {
        next;
    }  
   

    #
    # Capture additional fields which should be part of
    # entry data source, as requested by user. The map uses
    # entry ID as key, and the value is formatted as follows:
    #   fld_name=fld_val
    # This is stored in map "$id_to_add_ent_flds"
    #
    my @extracted_add_ent_flds;
    for my $add_ent (@add_entry_flds) {
        my ($fld_name, $fld_pos) = split('=', $add_ent);
        push(@extracted_add_ent_flds, $fld_name . '=' . $flds[$fld_pos]);
    }
    $id_to_add_ent_flds{$ENTRY_ID} = \@extracted_add_ent_flds;


    ++$ENTRY_ID;
}
close(IN);
undef $ENTRY_ID;

my @combo_idx_ary = ();
if (defined $combo_list_flds && $combo_list_flds !~ /^$/) {
    @combo_idx_ary = @{turnDelimSeparatedStringToList('\|', $combo_list_flds)};
}


print STDERR "$DEBUG Generating buckets for each entry...\n";
my %lists;
open(LISTS, $lists_file) || die "Could not open the file that will be use to generate the various list ($lists_file): $!";
while(<LISTS>) {
    s/[\n\r]+$//;

    my @flds = split(',');

    my $entry = entry(\@flds, $entry_delim);

    # Remove leading and trailing spaces
    $entry =~ s/^\s+//;
    $entry =~ s/\s+$//;

    #
    # If so requested, sets up individual lists for each 
    # attribute value encountered
    #
    if (defined $gen_indiv_lists && $gen_indiv_lists !~ /^$/) {
        for (my $i = 0; $i < @flds-1; $i++) {
            # Create list name using template that corresponds
            # to this field position
            my $list_name = createListName($list_tmpls{$i}, $flds[$i]);
    
            addEntryToList($list_name, $entry, \%lists);

            # The "allCombos" list gets mapped to all the entries; it's
            # a list that applies to all IE records
            addEntryToList($ALL_COMBOS_LIST_NAME, $entry, \%lists);
        }
    }


    #
    # Form lists (aka buckets) for each of the requested attribute combinations
    #
    for my $combo_idx (@combo_idx_ary) {
        my @idx_ary = split(',', $combo_idx);
        my $list_name = "";
        for my $i (@idx_ary) {
            $list_name .= createListName($list_tmpls{$i}, $flds[$i]); 
            $list_name .= '-';
        }
        chop($list_name);
        addEntryToList($list_name, $entry, \%lists);
    }
}
close(LISTS);
print STDERR "$DEBUG Done.\n";



#
# Generate list ID's and build mapper entries
#
my %mapper_entries;
my $LIST_ID = 0;
if ($list_start_id) {
    $LIST_ID = $list_start_id;
}
my %list_to_id;
print STDERR "$DEBUG Generating list ID's and mapper entries...\n";
for my $list_name (sort keys %lists) {
    $list_to_id{$list_name} = $LIST_ID;
    my @ent_ids = @{$lists{$list_name}};

    for my $id (@ent_ids) {
        my $e = $id_to_entry{$id};

        #
        # Build mapper entries
        #
        my @mapped_lists;
        if (exists $mapper_entries{$id}) {
            @mapped_lists = @{$mapper_entries{$id}}; 
        }
        push(@mapped_lists, $LIST_ID);
        $mapper_entries{$id} = \@mapped_lists;
    }
    ++$LIST_ID;
}
print STDERR "$DEBUG Done.\n";



#
# Finally print everything out
#
# Print out the lists
print STDERR "$DEBUG Printing out the lists...\n";
open(LISTS, ">./list.txt") || die "Could not open lists file for output: $!";
print LISTS "The Lists:\n";
my $list_table_tag_open = $TAB_TAG_OPEN;
my $list_table_name = $group_name . '_list_table';
$list_table_tag_open =~ s/<TABLE_NAME>/$list_table_name/;
print LISTS $list_table_tag_open;
for my $list_name (sort keys %list_to_id) {
    print LISTS qq/{"id":"$list_to_id{$list_name}","list_name":"$list_name"},\n/;
} 

print LISTS $TAB_TAG_CLOSE;
close(LISTS);


# Print out entry list
open(ENTS, ">./entry.txt") || die "Could not open entries file for output: $!";
print ENTS "The Entries:\n";
my $entry_table_tag_open = $TAB_TAG_OPEN;
my $entry_table_name = $group_name . '_entry_table';
$entry_table_tag_open =~ s/<TABLE_NAME>/$entry_table_name/;
print ENTS $entry_table_tag_open;
for my $ent_id (sort {$a <=> $b} keys %id_to_entry) {
    my $e = $id_to_entry{$ent_id};
    $e =~ s/\*/\./g;
    my $ent_row = qq/{"id":"$ent_id","entry":"$e"/;

    my $add_ent_flds = $id_to_add_ent_flds{$ent_id};
    if ($add_ent_flds) {
        for my $add_ent (@$add_ent_flds) {
            my ($fld_name, $fld_val) = split('=', $add_ent);
            $ent_row .= qq/,"$fld_name":"$fld_val"/;
        }
    }

    $ent_row .= qq/},/;
    print ENTS "$ent_row\n";
} 
print ENTS $TAB_TAG_CLOSE; 
close(ENTS);



# Print out mapper entries
open(MAPPER, ">./mapper.txt"); 
print MAPPER "The list-to-entries mapper rows:\n"
  || die "Could not open mapper file for output: $!";
my $entry_list_mapper_table_tag_open = $TAB_TAG_OPEN;
my $entry_list_mapper_table_name = $group_name . '_mapper_table';
$entry_list_mapper_table_tag_open =~ s/<TABLE_NAME>/$entry_list_mapper_table_name/;
print MAPPER $entry_list_mapper_table_tag_open; 
for my $ent_id (sort keys %mapper_entries) {
    my $mapped_lists_str = '[';
    for my $list_id (@{$mapper_entries{$ent_id}}) {
        $mapped_lists_str .= "$list_id,";
    }
    chop($mapped_lists_str);
    $mapped_lists_str .= ']';
    print MAPPER qq/{"$list_table_name":$mapped_lists_str,"$entry_table_name":"$ent_id"},\n/;
}
print MAPPER $TAB_TAG_CLOSE;
close(MAPPER);


#
# Sub-routines
#
sub entry {
    my $flds = $_[0];
    my $delim = $_[1];
    # The entry is made up of one or more fields explicitly specified by user,
    # else it's just the last field in the input file given.
    my $entry = '';
    my @entry_fld_vals;
    if (@entry_fld_idx > 0) {
        for my $idx (@entry_fld_idx) {
            push(@entry_fld_vals, $flds->[$idx]);
            $entry = $entry . $flds->[$idx] . $delim;
        }

        if ($sort_entry_vals) {
            @entry_fld_vals = sort(@entry_fld_vals); 
        }

        $entry = join($delim, @entry_fld_vals);
    } else {
        $entry = $flds->[$#$flds];
    }
    return $entry
}


sub addEntryToList {
    my ($list_name, $entry, $list_to_entry_map) = @_;

    my @list_entries;
    if (exists $lists{$list_name}) {
        @list_entries = @{$lists{$list_name}};
    }

    # Do nothing if the given list name is already mapped
    # to this entry
    if (listContainsValue(\@list_entries, translateEntryToUniqueId($entry))) {
        return; 
    }

    push @list_entries, translateEntryToUniqueId($entry);

    $list_to_entry_map->{$list_name} = \@list_entries;
    print STDERR "$DEBUG Added $entry to $list_name.\n";
}



sub listContainsValue {
    my ($the_list_ref, $val) = @_;
    my @list = @$the_list_ref;
    my %list_ents = map {$_ => 1} @list;
    return exists($list_ents{$val});
}



sub createListName {
    my ($tmpl, $attr_val) = @_;

    my $list_name = $tmpl;

    if (!defined $attr_val || $attr_val =~ /^$/) {
        $attr_val = 'All'; 
    }

    # The $placeholder replacement bit is here for backward 
    # compatibility, to support old style of specifying
    # attributes to use to create lists (I.e. buckets)
    my $placeholder = '__VAL__';
    if ($list_name =~ /$placeholder/) {
        $list_name =~ s/$placeholder/$attr_val/;
    } else {
        $list_name .= $attr_val;
    }

    return $list_name;
}


#
# 
#
sub translateEntryToUniqueId {
    my $entry = $_[0];
    exists($entry_to_id{$entry}) || die "Entry $entry is not mapped to ID";
    return $entry_to_id{$entry};
}


sub turnDelimSeparatedStringToList {
    my $delim = $_[0];
    my $str = $_[1];
    my @ary = split($delim, $str);

    return \@ary;
}


sub buildHashMapFromString {
    my $list_delim = $_[0];
    my $token_delim = $_[1];
    my $str = $_[2];
    my @ary = @{turnDelimSeparatedStringToList($list_delim, $str)};

    my %hash;
    my $idx = 0;
    for my $e (@ary) {
        my @t = split($token_delim, $e);
        if (@t == 2) {
            $hash{$t[1]} = $t[0]; 
        } elsif (@ == 1) {
            $hash{$idx} = $t[0];
        } else {
            die "Got invalid number of tokens in list entry, string is $str\n";
        }
        ++$idx;
    } 

    return \%hash;
}


sub addExistingEntries {
    my $file = $_[0];
    my $ENTRY_PROP_NAME = 'entry';
    my $ID_PROP_NAME = 'id';
    open(IN, "$file") || die "Could not open existing entry universe file $file: $!";
    while(<IN>) {
        my $cur_ln = $_;
        if ($cur_ln !~ /$ENTRY_PROP_NAME/ && $cur_ln !~ /"$ID_PROP_NAME"/) {
            next;
        }
 
        s/[\n\r]+$//;
        my ($id) = ($cur_ln =~ /$ID_PROP_NAME"\s*:\s*"([^"]+)"/);
        
        while ($cur_ln !~ /$ENTRY_PROP_NAME/) {
            $cur_ln = <IN>;    
        }

        # Keep track of the last ID added by existing entries, so that new 
        # entries can pick up ID from there
        if ($id > $ENTRY_ID) {
            $ENTRY_ID = $id+1;
        }
        my ($e) = ($cur_ln =~ /$ENTRY_PROP_NAME"\s*:\s*"([^"]+)"/); 
        if (not addEntry($id, $e, \%entry_to_id, \%id_to_entry)) {
            next;
        }

        print STDERR "DEBUG: Added existing entry $e, id is $id\n";
    }
    close(IN);
    print STDERR "DEBUG: New entries will start from ID $ENTRY_ID\n";
}


sub addEntry {
    my ($id, $ent, $ent_to_id, $id_to_ent) = @_;

    # Remove leading and trailing spaces
    $ent =~ s/^\s+//;
    $ent =~ s/\s+$//;

    # Skip already seen ones
    if (exists $ent_to_id->{$ent}) {
        return undef;
    }


    $ent_to_id->{$ent} = $id;
    $id_to_ent->{$id} = $ent;

    return 1;
}
