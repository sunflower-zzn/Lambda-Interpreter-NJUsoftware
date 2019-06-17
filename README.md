# Lambda Interpreter
南京大学2018级软工（一）期末大作业：lambda解释器

代码参考：

[200行JS代码实现lambda解释器](https://github.com/tadeuzagallo/lc-js)

[PP大佬代码](https://github.com/pppppkun/LambdaInterpreter)
## 解释器构造
一个lambda interpreter主要由一下几个方面构成：
+ 词法分析器（Lexer）：将字符流分解为符号流（token流）
+ 语法分析器（Parser）：根据语法，利用符号流构建抽象语法树AST
+ 语法解释器/语法制导的翻译（Interpreter）：遍历处理AST，进行对语法树进行求值

其他工具类：
- TokenType：枚举类，token种类
- AST：接口，实现语法树`tostring()`方法，有三个子类（根据语法规则）
  - Application
  - Abstraction
  - Identifier
## TokenType
```
LPAREN: '('
RPAREN: ')'
LAMBDA: '\' // 为了方便使用 “\”
DOT: '.'
LCID: /[a-z][a-zA-Z]*/ 
EOF：输入流终止
```
## Lexer
处理 token 的辅助方法：(可以自行定义)
+ nextChar()：读入下一个字符
+ nextToken()：读入下一个token的type和value（LCID），跳过空白符
+ next(Token t)：判断下一个token是否为t,如果true就打印token，否则就不打印并恢复index
+ match(Token t)：断言下一个token为 t（即next(t)返回为true）,否则报错退出程序【这里可以加上异常处理，我只做了报错退出】

注：调用next和match是如果为true就向控制台输出Token类型+换行
## Parser
###### 语法规则
[BNF范式（巴科斯范式）简介](https://www.cnblogs.com/huiyenashen/p/4445676.html)
```
1.Term ::= Application| LAMBDA LCID DOT Term

2.Application ::= Application Atom| Atom
//这里需要做一个特殊处理，本条规则为左递归，会导致无限递归，因此将其处理为右递归
application ::= atom application'
application' ::= atom application'| ε

3.Atom ::= LPAREN Term RPAREN| LCID
```
##### 抽象语法树 AST构造
lambda 演算的 AST 非常简单，因为我们只有 3 种节点： 
+ Abstraction （抽象）："  \x.  t1  "
+ Application （应用）："  t1  t2  "
+  Identifier （标识符）："  x  "
```
Abstraction 
    Identifier param;//变量
    AST body;//表达式
toString显示为： \.body.toString()
```
```
Application
    AST lhs;左树
    AST rhs;右树
toString显示为： (lhs.toString()空格rhs.toString())
```
```
Indentifier
	String name;//变量名
    String value；//De Bruijn index
toString显示为： value
```
##### De Bruijn index
使用德布鲁因值来避免变量重名导致的规约结果不同
```
(\x.\y.x \f.\g.g)
首先转化为：（变量保持不变，数字从0开始代码同层变量，1代表上一层次变量。）
(\x.\y.1 \f.\g.0)
toString显示为：（为了防止alpha变换造成的不一致，去掉变量）
(\.\.1 \.\.0)
```
## Interpreter
##### 求值evalAST（递归思路）
evalAST规则：
+ 首先检测ast是否为 application，如果是，则判断左树类型：
	- 如果左树是Abstraction，将右树作为param代换入左树的body表达式,替代掉最外层的变量
	- 如果左树是Application，对左右树分别单独求值，并进一步判断求值结果形式
	- 如果左树是Identifier，对右树单独求值并更新右树
+ 如果ast是Abstraction，则对ast的body表达式部分求值
+ 如果ast是Identifier，则直接返回叶节点

##### substitute
这里需要做一个处理，因为代换以后最外层会消掉一个lambda，所以所有变量要shift位移-1，而value中的叶节点代入是就需要补位移一个1；
##### subst变量代换
- 如果节点是Applation，分别对左右树替换；
- 如果node节点是abstraction，替入node.body时深度outDepth+1；
- 如果node是identifier，则替换De Bruijn index值等于outDepth的identifier（替换之后value的值加深outDepth）

##### shift德布鲁因值位移
- 如果节点是Applation，分别对左右树位移；
- 如果value节点是abstraction，新的body等于旧value.body位移by（inDepth得+1）；
- 如果value是identifier，则新的identifier的De Bruijn index值如果大于等于inDepth则加by，否则加0（超出内层的范围的外层变量才要shift by位）

## Main方法

```
public static void main(String[] args) {

        String source = "(\\x.\\y.x)(\\x.x)(\\y.y)";

        Lexer lexer = new Lexer(source);

        Parser parser = new Parser(lexer);

        Interpreter interpreter = new Interpreter(parser);

        AST result = interpreter.eval();

        System.out.println(result.toString());
}
```
## Test
```

    @Test
    public void testLexer() {
        Lexer lexer = new Lexer(sources[1]);
        Parser parser = new Parser(lexer);
        AST ast = parser.parse();

        assertEquals("LPAREN" + lineBreak+
                "LPAREN" + lineBreak+
                "LAMBDA" + lineBreak+
                "LCID" + lineBreak+
                "DOT" + lineBreak+
                "LAMBDA" + lineBreak+
                "LCID" + lineBreak+
                "DOT" + lineBreak+
                "LAMBDA" + lineBreak+
                "LCID" + lineBreak+
                "DOT" + lineBreak+
                "LCID" + lineBreak+
                "LPAREN" + lineBreak+
                "LCID" + lineBreak+
                "LCID" + lineBreak+
                "LCID" + lineBreak+
                "RPAREN" + lineBreak+
                "RPAREN" + lineBreak+
                "LPAREN" + lineBreak+
                "LAMBDA" + lineBreak+
                "LCID" + lineBreak+
                "DOT" + lineBreak+
                "LAMBDA" + lineBreak+
                "LCID" + lineBreak+
                "DOT" + lineBreak+
                "LCID" + lineBreak+
                "RPAREN" + lineBreak+
                "RPAREN"+lineBreak+
                "EOF"+lineBreak,bytes.toString());

    }

    @Test
    public void testParser() {
        Lexer lexer = new Lexer(sources[1]);
        Parser parser = new Parser(lexer);
        AST ast = parser.parse();
        assertEquals("(\\.\\.\\.(1 ((2 1) 0)) \\.\\.0)",ast.toString());


    }

    @Test
    public void testInterpreter() {
        Lexer lexer = new Lexer(sources[1]);
        Parser parser = new Parser(lexer);
        interpreter = new Interpreter(parser);

        AST ast = parser.parse();

        AST result = interpreter.eval(ast);

        assertEquals("\\.\\.(1 0)",result.toString());


    }
``````