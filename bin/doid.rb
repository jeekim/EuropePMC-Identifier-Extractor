#!/usr/bin/env ruby
# encoding: utf-8
bigger_than = ARGV[0].to_i

# print head
puts <<HEAD
<mwt>
  <template><z:doid ids="%1" cat="%2">%0</z:doid></template>
HEAD

# print body
# lines.drop(1).each do |line|
STDIN.readlines.drop(1).each do |line|
  a = line.split("\t").map { |e| if /\"(.+)\"/ =~ e then $1 else e end }
  id = if /\/([^\/]+)>/ =~ a[0] then $1 else a[0] end

  cat = if a[4] == "d" then "disease"
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
