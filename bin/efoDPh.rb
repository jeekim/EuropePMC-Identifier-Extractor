#!/usr/bin/env ruby
# encoding: utf-8
# lines = IO.readlines("tsv/efoDPh2_68.tsv")
bigger_than = ARGV[0].to_i
# bigger_than = 2

# print head
puts <<HEAD
<mwt>
  <template><z:efo ids="%1" cat="%2">%0</z:efo></template>
HEAD

puts ("  <t p1=\"EFO_0003767\" p2=\"disease\">IBD</t>") if (bigger_than == 3)
# print body
# lines.drop(1).each do |line|
STDIN.readlines.drop(1).each do |line|
  a = line.split("\t").map { |e| if /\"(.+)\"/ =~ e then $1 else e end }
  id = if /\/([^\/]+)>/ =~ a[0] then $1 else a[0] end

  cat = if a[4] == "p" then "phenotype"
        elsif a[4] == "d" then "disease"
        else "other"
        end

  term = a[2]

  term = term.gsub(/\(morphologic abnormality\)/, '')
  term = term.gsub(/\(finding\)/, '')
  term = term.gsub(/\[Ambiguous\]/, '')
  # term = term.gsub(/\(morphologic abnormality\)/, 'morphologic abnormality')
  # term = term.gsub(/\(finding\)/, 'finding')
  # term = term.gsub(/\[Ambiguous\]/, 'Ambiguous')

  if (/([Dd]elta|[Aa]lpha|[Bb]eta|[Gg]amma)/ =~ term) then
    puts ("  <t p1=\"#{id}\" p2=\"#{cat}\">#{term.strip}</t>")
    term = term.gsub(/[Aa]lpha/, 'α')
    term = term.gsub(/[Bb]eta/, 'β')
    term = term.gsub(/[Gg]amma/, 'γ')
    term = term.gsub(/[Dd]elta/, "δ")
  end

  #term = term.gsub(/Delta/, 'δ')
  #term = term.gsub(/Alpha/, 'α')
  #term = term.gsub(/Beta/, 'β')
  #term = term.gsub(/Gamma/, 'γ')

  if (/\(disorder\)/ =~ term) then
    term1 = term.gsub(/\(disorder\)/, 'disorder')
    term2 = term.gsub(/\(disorder\)/, '')
    puts ("  <t p1=\"#{id}\" p2=\"#{cat}\">#{term1.strip}</t>")
    puts ("  <t p1=\"#{id}\" p2=\"#{cat}\">#{term2.strip}</t>")
  elsif (id == "EFO_0003765") then
    ;
  elsif (id == "EFO_0000545" and term == "sterile") then
    ;
  else
    puts ("  <t p1=\"#{id}\" p2=\"#{cat}\">#{term.strip}</t>") if (term.size > bigger_than)
  end
end

# print tail
puts <<TAIL
  <template>%0</template>
    <r><z:[^>]*>(.*</z)!:[^>]*></r>
</mwt>
TAIL
