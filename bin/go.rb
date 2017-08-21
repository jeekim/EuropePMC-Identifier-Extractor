#!/usr/bin/ruby
#lines = IO.readlines("go150429.tsv")

# print head
puts <<HEAD
<mwt>
  <template><z:go ids="%1" onto="%2">%0</z:go></template>
HEAD

ARGF.drop(1).each do |line|
  a = line.split("\t").map { |e| if /\"(.+)\"/ =~ e then $1 else e end }
  cat = if a[4] == "bp" then "biological_process"
          elsif a[4] == "mf" then "molecular_function"
	  elsif a[4] == "cc" then "cellular_component"
          else "whatelse?"
        end
  puts ("  <t p1=\"#{a[0]}\" p2=\"#{cat}\">#{a[2]}</t>") if a[2].size > 3
end

puts <<TAIL
  <template>%0</template>
    <r><z:[^>]*>(.*</z)!:[^>]*></r>
</mwt>
TAIL
