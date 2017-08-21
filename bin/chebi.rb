#!/usr/bin/ruby
#lines = IO.readlines("chebi150428.tsv")

# print head
puts <<HEAD
<mwt>
  <template><z:chebi ids="%1" onto="%2">%0</z:chebi></template>
HEAD

ARGF.drop(1).each do |line|
  a = line.split("\t").map { |e| if /\"(.+)\"/ =~ e then $1 else e end }
  cat = if a[4] == "ce" then "chemical_entity"
          elsif a[4] == "r" then "role"
          elsif a[4] == "sp" then "subatomic_particle"
          else "whatelse?"
        end
  puts ("  <t p1=\"#{a[0]}\" p2=\"#{cat}\">#{a[2]}</t>") if a[2].size > 3
end

puts <<TAIL
  <template>%0</template>
    <r><z:[^>]*>(.*</z)!:[^>]*></r>
</mwt>
TAIL
