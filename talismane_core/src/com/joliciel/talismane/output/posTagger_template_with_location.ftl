[#ftl]
[#list sentence as unit]
[#compress]
[#if unit.token.precedingRawOutput??]${unit.token.precedingRawOutput}[/#if]
${(unit.token.index+1)?c}	${unit.token.textForCoNLL}	${unit.lemmaForCoNLL}	${unit.tag.code}	${unit.tag.code}	${(unit.lexicalEntry.morphologyForCoNLL)!"_"}	${unit.token.fileName}	${(unit.token.lineNumber)?c}	${(unit.token.columnNumber)?c}	${(unit.token.lineNumberEnd)?c}	${(unit.token.columnNumberEnd)?c}	
[#if unit.token.trailingRawOutput??]${unit.token.trailingRawOutput}[/#if]
[/#compress]

[/#list]

