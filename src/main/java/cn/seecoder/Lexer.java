package cn.seecoder;


import jdk.nashorn.internal.parser.Token;

public class Lexer{

    public String source;
    public int index;
    public TokenType token;
    public String tokenvalue;
    private int len_LCID;

    public Lexer(String s){
        index = 0;
        source = s;
        len_LCID=0;
    }

    //读入下一个字符
    private char nextChar(){
        char[] ch=source.toCharArray();
        char c;
        if(index<ch.length){
            c=ch[index];
            index++;
        }
        else{
            c='\0';
            index++;
        }
        return c;
    }

    //读入下一个token
    private void nextToken() {
        char c = nextChar();
        switch (c) {
            case ' ':{    //处理空格
                nextToken();
                break;
            }
            case '(': {
                token = TokenType.LPAREN;
                break;
            }
            case ')': {
                token = TokenType.RPAREN;
                break;
            }
            case '\\': {
                token = TokenType.LAMBDA;
                break;
            }
            case '.': {
                token = TokenType.DOT;
                break;
            }
            case '\0':{
                token= TokenType.EOF;
                break;
            }
            default: {
                if (c >= 'a' && c <= 'z') {
                    token = TokenType.LCID;
                    len_LCID=0;
                    tokenvalue = "";
                    //变量名可能为字符串，使用逐个读入加字符串拼接的方法处理
                    do {
                        tokenvalue += c;
                        len_LCID++;//用于统计变量字节数
                        c = nextChar();
                    } while ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'));
                    index--;

                }

                else {
                    token = TokenType.EOF;
                }
            }
        }
    }

    //判断下一个token是否为t,如果true就打印token,否则就不打印并恢复index
    public boolean next(TokenType t){
        nextToken();
        if(token.equals(t)){
            System.out.println(t);
            return true;
        }
        else{
            if(t.equals(TokenType.LCID)){
                index-=len_LCID;
            }
            else{
                index--;
            }
            return false;
        }
    }

    //断言下一个token为t(next(t)返回为true),否则报错退出程序
    public void match(TokenType t){
        if(!next(t)) {
            System.out.print("匹配 "+t.toString()+" 时出现错误!");
            System.exit(-1);
        }
    }





}
