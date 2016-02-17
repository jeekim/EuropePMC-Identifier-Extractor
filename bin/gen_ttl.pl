#!/usr/bin/perl -w
# based on slide ao_diagram4whatizitV7

use strict;

my $pipeline_v = "xxyyzz";

# namespaces
my $dcterms = "http://purl.org/dc/terms/";
my $orb = "http://purl.org/orb/";
my $oa = "http://www.w3.org/ns/oa#";
my $rdf = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
my $rdfs = "http://www.w3.org/2000/01/rdf-schema#";
my $pmc = "http://europepmc.org/articles/";
my $pmc_rdf = "http://rdf.ebi.ac.uk/resource/europepmc/annotations/";
my $string = "http://www.w3.org/2001/XMLSchema#string";

my @lines;
my @chunks;
my @sids; # a list of sentence ids.

# 
&initiate();

while (my $line = <>) {
  chomp $line; # mutable? yes.

  if ($line =~ /^\/\/$/) { # end of each article
    &finish();
    &process();
    &initiate();
  } else {
    my ($pid, $sid, $cid, $section, $zone, $size, $chunk, $sem_type, $sem_id) = split /\t/, $line;
    $chunk = &escape($chunk);

    push @lines, $line;
    push @chunks, $chunk;
    push @sids, $sid;
  }
}

sub initiate {
  @lines = (0);
  @chunks = (0);
  @sids = (0);
}

sub finish {
  push @lines, -1;
  push @chunks, -1;
  push @sids, -1;
}

sub process { # a list of lines in an article
  for (my $i = 1; $i < @lines - 1; $i++) {
    my $line = $lines[$i];
    my ($pid, $sid, $cid, $section, $zone, $size, $chunk, $sem_type, $sem_id) = split /\t/, $line;
    $chunk = &escape($chunk);
    my $key = "${pid}#${sid}-${cid}";
    my $p_chunk = &get_p($i); # within a sentence boundary
    my $n_chunk = &get_n($i);

    # if ($sem_id) {
    # if ($sem_id and ($sem_id =~ /(chebi)/) and ($sem_id ne "chebi:16541")) {
    if ($sem_id and ($sem_id !~ /(efo|chebi)/)) {
      &write_a($key, $sem_id, $chunk); 
      &write_b($key, $sem_id); 
      &write_t($key, $pid, $section); 
      # &write_t($key, $pid); 
      &write_s($key, $chunk, $p_chunk, $n_chunk);
    }
  }
  print "\n";
}

sub get_p { # get the previous chunk
  my $i = shift;
  my $sid = $sids[$i];
  my $s = $i - 1;

  while ($sid == $sids[$s]) { $s = $s - 1; }
  my $p_chunk = join ("", @chunks[($s + 1) .. ($i - 1)]) || "";

  return $p_chunk;
}

sub get_n { # get the next chunk
  my $i = shift;
  my $sid = $sids[$i];
  my $e = $i + 1;

  while ($sid == $sids[$e]) { $e = $e + 1; }
  my $n_chunk = join ("", @chunks[($i + 1) .. ($e - 1)]) || "";

  return $n_chunk;
}

sub get_sec {
  my ($osec) = @_;

  my %map = (
    'INTRO' => 'Introduction',
    'METHODS' =>'Methods',
    'RESULTS' => 'Results',
    'DISCUSS' => 'Discussion',
    'FIG' => 'Figure',
    'TABLE' => 'Table',
    'REF' => 'References',
    'ACK_FUND' => 'Acknowledgments',
  );

  return $map{$osec} || "";
}

sub get_uri {
  my ($sem_id) = @_;
  my ($db, $id, $uri, $acc_db);

  my $dbs = "(uniprot|GO|chebi|species|efo|disease|acc)[:_](GO:[0-9]+|[\.a-zA-Z0-9_]+)([:a-zA-Z]*)";

  # uri for cross-refs
  my %links = (
    'uniprot' => "http://purl.uniprot.org/uniprot/",
    'species' => "http://identifiers.org/taxonomy/",
    'chebi' => "http://purl.obolibrary.org/obo/CHEBI_",
    'GO' => "http://purl.obolibrary.org/obo/GO:",
    'disease' => "http://linkedlifedata.com/resource/umls-concept/",
    'efo' => "http://www.ebi.ac.uk/efo/",
    'acc' => "http://identifiers.org/",
  );

  my %id_map = ( # 20 database types
    'gen' => "ena.embl",
    'refsnp' => "dbsnp", # http://www.ncbi.nlm.nih.gov/SNP/snp_ref.cgi?rs=rs4686484
    'pdb' => "pdb",
    'refseq' => "refseq",
    'nct' => "clinicaltrials",
    'omim' => "omim",
    'go' => "go",
    'sprot' => "uniprot",
    'pfam' => "pfam",
    'arrayexpress' => "arrayexpress",
    'doi' => "doi",
    'ensembl' => "ensembl",
    'bioproject' => "bioproject",
    'interpro' => "interpro",
    'eudract' => "euclinicaltrials",
    'pxd' => "proteomexchange",
    'biosample' => "biosample",
    'emdb' => "emdb",
    'treefam' => "treefam",
    'ega' => "ega.dataset",
  );

  if ($sem_id =~ /$dbs/) {
    $db = $1;
    $id = $2;
    if ($3) {
      $acc_db = $3;
    }
  } else {
    die "no db in $dbs matched with $sem_id ...\n";
  }

  if ($links{$db}) {
    if ($acc_db) {
      $acc_db =~ s/^://;
      $acc_db = $id_map{$acc_db} or die "$acc_db should be mapped!\n";
      $uri = "$links{$db}$acc_db/$id";
    } else {
      $uri = "$links{$db}$id";
    }
  } else {
    die "no uri for $db ...";
  }

  return $uri;
}

### subroutines for generating triples ###
sub write_a { # annotation
  my ($key, $sem_id, $chunk) = @_;
  $sem_id = &get_uri($sem_id);

  # print "<${pmc_rdf}$key> <${rdf}type> <${oa}Annotation> .\n";
  # print "<${pmc_rdf}$key> <${oa}motivatedBy> <${oa}tagging> .\n";
  print "<${pmc_rdf}$key> <${oa}hasBody> <$sem_id> .\n";
  print "<${pmc_rdf}$key> <${oa}hasTarget> <${pmc}${key}t> .\n";
  # print "<${pmc_rdf}$key> <${rdfs}label> \"$chunk\"^^<$string> .\n";
  # print "<${pmc_rdf}$key> <${oa}hasTarget> _:t$key .\n";
}

sub write_b { # body
  my ($key, $sem_id) = @_;
  $sem_id = &get_uri($sem_id);
  
  # print "<$sem_id> <${rdf}type> <${oa}SemanticTag> .\n";
  # print "_:b$key <${foaf}page> <$sem_id> .\n";
}

sub write_t { # target
  my ($key, $pid, $section) = @_;
  $section = &get_sec($section);

  # print "<${pmc}${key}t> <${rdf}type> <${oa}SpecificResource> .\n";
  print "<${pmc}${key}t> <${oa}hasSource> <${pmc}$pid> .\n";
  print "<${pmc}${key}t> <${oa}hasSelector> <${pmc}${key}s> .\n";
  print "<${pmc}${key}t> <${dcterms}isPartOf> <${orb}$section> .\n" if ($section);
}

sub write_s { # selector
  my ($key, $chunk, $p_chunk, $n_chunk) = @_;
  # print "<${pmc}${key}s> <${rdf}type> <${oa}TextQuoteSelector> .\n";
  print "<${pmc}${key}s> <${oa}exact> \"$chunk\"^^<$string> .\n";
  print "<${pmc}${key}s> <${oa}prefix> \"$p_chunk\"^^<$string> .\n";
  print "<${pmc}${key}s> <${oa}postfix> \"$n_chunk\"^^<$string> .\n";
}

sub escape {
  my ($temp) = @_;
  # (my $chunk = $temp) =~ s/"/'\\\"'/g;
  (my $temp1 = $temp) =~ s/\\/\\\\/g;
  (my $chunk = $temp1) =~ s/"/\\\"/g;
  # TODO implement based on http://www.dajobe.org/2004/01/turtle/2006-12-04/#sec-strings
  return $chunk;
}

# end
