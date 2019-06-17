package cn.seecoder;

public class Interpreter {
    Parser parser;
    AST astAfterParser;

    public Interpreter(Parser p) {
        parser = p;
        astAfterParser = p.parse();
        //System.out.println("After parser:"+astAfterParser.toString());
    }

    //java 中的instanceof 运算符是用来在运行时指出对象是否是特定类的一个实例。
    private boolean isAbstraction(AST ast) { return ast instanceof Abstraction; }

    private boolean isApplication(AST ast) { return ast instanceof Application; }

    private boolean isIdentifier(AST ast) { return ast instanceof Identifier; }


    //public方法,供给调用
    public AST eval() { return evalAST(astAfterParser); }

    //private方法,进行语法树处理
    private AST evalAST(AST ast) {
        //ast求值
        while (true) {

            //如果ast是Application形式:"  t1  t2  ",判断左右树结构
            //左树可能为Abstraction/Application/Identifier
            //右树可能为Abstraction/Application/Identifier
            if(ast instanceof Application){

                //如果左边的树为Abstraction形式:"  \x.  t1  "
                if(isAbstraction(((Application) ast).getLhs())){
                    //将右树作为param代换入左树的body表达式,替代掉最外层的变量
                    ast = substitute(((Abstraction)((Application) ast).getLhs()).getBody(),((Application) ast).getRhs());
                }

                //如果左树是Application形式:"  t1  t2  "
                else if(isApplication(((Application) ast).getLhs())){
                    //对左树单独求值并更新左树
                    ((Application) ast).setLhs(evalAST(((Application) ast).getLhs()));
                    //对右树单独求值并更新右树
                    ((Application) ast).setRhs(evalAST(((Application) ast).getRhs()));

                    //如果左树是Abstraction形式:"  \x.  t1  ",又可以进行代换了
                    if(isAbstraction(((Application) ast).getLhs())){
                        //对ast重新求值(递归思路)
                        ast = evalAST(ast);
                    }
                    //所有处理完成,不能再求值,返回结果
                    return ast;
                }

                //如果左树是Identifier形式:"  x  "
                else{
                    //对右树单独求值并更新右树
                    ((Application) ast).setRhs(evalAST(((Application) ast).getRhs()));
                    return ast;
                }
            }

            //如果ast是Abstraction形式:"  \x.  t1  "
            else if(isAbstraction(ast)){
                //处理Abstraction形式:"  \x.  t1  "的表达式部分(递归直至返回Identifier)
                ((Abstraction) ast).setBody(evalAST(((Abstraction) ast).getBody()));
                return ast;
            }

            //如果ast是Identifier形式:"  x  "
            else{
                //无法继续求值,返回叶节点
                return ast;
            }

        }


    }

    //将value代换入node节点(body)中的param变量
    private AST substitute(AST node,AST value){
        return shift(-1,subst(node,shift(1,value,0),0),0);
    }


    /**
     *  value替换node节点中的变量：
     *  如果节点是Applation，分别对左右树替换；
     *  如果node节点是abstraction，替入node.body时深度outDepth+1；
     *  如果node是identifier，则替换De Bruijn index值等于outDepth的identifier（替换之后value的值加深outDepth）

     *@param value 替换成为的value
     *@param node 被替换的整个节点
     *@param outDepth 外围的深度
           
     *@return AST
     *@exception  (方法有异常的话加)
     *
     */
    //value替换node节点中的变量：
    private AST subst(AST node, AST value, int outDepth){
        if(isApplication(node)){
            //如果node是Application,对左右树分别替换
            ((Application) node).setLhs(subst(((Application)node).getLhs(),value,outDepth));
            ((Application) node).setRhs(subst(((Application)node).getRhs(),value,outDepth));
            return node;
        }
        else if(isAbstraction(node)){
            //如果node是Abstraction,value替换node的body中的变量,相当于向内深入了一层,outDepth+1
            ((Abstraction) node).setBody(subst(((Abstraction)node).getBody(),value,++outDepth));
            return node;
        }
        else{
            //如果node是Identifier,替换与value的德布鲁因值相同的叶节点,否则原样返回
            if(outDepth==Integer.valueOf(((Identifier)node).getValue())){
                //替换的同时也要改变value的德布鲁因值
                return shift(outDepth,value,0);
            }
            else{
                return node;
            }
        }
    }


    /**

     *  De Bruijn index值位移
     *  如果节点是Applation，分别对左右树位移；
     *  如果value节点是abstraction，新的body等于旧value.body位移by（inDepth得+1）；
     *  如果value是identifier，则新的identifier的De Bruijn index值如果大于等于inDepth则加by，否则加0（超出内层的范围的外层变量才要shift by位）
     *
     *  特别注意:这里shift需要返回new的AST,而不能直接返回value,否则会导致爆栈!!!
     *  可能的原因,每次调用shift都会在原有语法树栈下延长,会导致空间长度不足爆栈,而new返回则是开辟新的栈所以安全(有点像多线程栈)

     *@param by 位移的距离
     *@param value 位移的节点
     *@param inDepth 内层的深度
             
     *@return AST
     *@exception  (方法有异常的话加)

     */
    //德布鲁因值的改变
    private AST shift(int by, AST value,int inDepth){
        /*new Thread(null, new Runnable() {
            int dep = 0;

            @Override
            public void run() {
                try {
                    d();
                } catch (Throwable e) {
                }
                System.out.println(dep);
            }

            void d() {
                dep++;
                d();
            }
        },"thread-1",1024*50).start();
        //千万不要轻易尝试!!!可能导致死机
        //默认分配栈2w,1024*1024*30能把栈设置为650w!!!
        */
        //如果value是Application,分别对左右树德布鲁因值改变
        if(isApplication(value)){
            /*(
            (Application) value).setLhs(shift(by,((Application)value).getLhs(),from));
            ((Application) value).setRhs(shift(by,((Application)value).getRhs(),from));
            return value;
            */
            return new Application(shift(by,((Application)(value)).getLhs(),inDepth), shift(by,((Application)(value)).getRhs(),inDepth));
        }
        //如果node是Abstraction,内部深度inDepth+1,body位移by
        else if(isAbstraction(value)){
            /*
            ((Abstraction)value).setBody(shift(by,((Abstraction)value).getBody(),from+1));
            return value;
            */
            return new Abstraction(((Abstraction) value).getParam(),shift(by,((Abstraction) value).getBody(),inDepth+1));
        }
        //如果value是Identifier
        else{
            //如果value的德布鲁因值大于等于内层深度,则加by,否则不变
            if(Integer.parseInt(((Identifier)value).getValue())>=inDepth){
                return new Identifier(((Identifier) value).getName(), String.valueOf(Integer.parseInt(((Identifier)value).getValue())+by));
            }
            else{
                return value;
                //return new Identifier(((Identifier) value).getName(), String.valueOf(Integer.parseInt(((Identifier)value).getValue())));
            }

        }





    }

    static String ZERO = "(\\f.\\x.x)";
    static String SUCC = "(\\n.\\f.\\x.f (n f x))";
    static String ONE = app(SUCC, ZERO);
    static String TWO = app(SUCC, ONE);
    static String THREE = app(SUCC, TWO);
    static String FOUR = app(SUCC, THREE);
    static String FIVE = app(SUCC, FOUR);
    static String PLUS = "(\\m.\\n.((m "+SUCC+") n))";
    static String POW = "(\\b.\\e.e b)";       // POW not ready
    static String PRED = "(\\n.\\f.\\x.n(\\g.\\h.h(g f))(\\u.x)(\\u.u))";
    static String SUB = "(\\m.\\n.n"+PRED+"m)";
    static String TRUE = "(\\x.\\y.x)";
    static String FALSE = "(\\x.\\y.y)";
    static String AND = "(\\p.\\q.p q p)";
    static String OR = "(\\p.\\q.p p q)";
    static String NOT = "(\\p.\\a.\\b.p b a)";
    static String IF = "(\\p.\\a.\\b.p a b)";
    static String ISZERO = "(\\n.n(\\x."+FALSE+")"+TRUE+")";
    static String LEQ = "(\\m.\\n."+ISZERO+"("+SUB+"m n))";
    static String EQ = "(\\m.\\n."+AND+"("+LEQ+"m n)("+LEQ+"n m))";
    static String MAX = "(\\m.\\n."+IF+"("+LEQ+" m n)n m)";
    static String MIN = "(\\m.\\n."+IF+"("+LEQ+" m n)m n)";

    private static String app(String func, String x){
        return "(" + func + x + ")";
    }
    private static String app(String func, String x, String y){
        return "(" +  "(" + func + x +")"+ y + ")";
    }
    private static String app(String func, String cond, String x, String y){
        return "(" + func + cond + x + y + ")";
    }

    public static void main(String[] args) {

        String[] sources = {
                ZERO,//0
                ONE,//1
                TWO,//2
                THREE,//3
                app(PLUS, ZERO, ONE),//4
                app(PLUS, TWO, THREE),//5
                app(POW, TWO, TWO),//6
                app(PRED, ONE),//7
                app(PRED, TWO),//8
                app(SUB, FOUR, TWO),//9
                app(AND, TRUE, TRUE),//10
                app(AND, TRUE, FALSE),//11
                app(AND, FALSE, FALSE),//12
                app(OR, TRUE, TRUE),//13
                app(OR, TRUE, FALSE),//14
                app(OR, FALSE, FALSE),//15
                app(NOT, TRUE),//16
                app(NOT, FALSE),//17
                app(IF, TRUE, TRUE, FALSE),//18
                app(IF, FALSE, TRUE, FALSE),//19
                app(IF, app(OR, TRUE, FALSE), ONE, ZERO),//20
                app(IF, app(AND, TRUE, FALSE), FOUR, THREE),//21
                app(ISZERO, ZERO),//22
                app(ISZERO, ONE),//23
                app(LEQ, THREE, TWO),//24
                app(LEQ, TWO, THREE),//25
                app(EQ, TWO, FOUR),//26
                app(EQ, FIVE, FIVE),//27
                app(MAX, ONE, TWO),//28
                app(MAX, FOUR, TWO),//29
                app(MIN, ONE, TWO),//30
                app(MIN, FOUR, TWO),//31
        };

        for(int i=0 ; i<sources.length; i++) {

            String source = sources[i];

            System.out.println(i+":"+source);

            Lexer lexer = new Lexer(source);

            Parser parser = new Parser(lexer);

            Interpreter interpreter = new Interpreter(parser);

            AST result = interpreter.eval();

            System.out.println(i+":" + result.toString());

        }

    }
}
