package cn.seecoder;

public class Identifier extends AST {

    private String name; //名字
    private String value;//De Bruijn index值

    public Identifier(String n,String v){

        name = n;
        value = v;
    }
    public String toString(){
        return value;
    }

    public String getName(){return name;}

    public String getValue(){return value;}

    public void setValue(String value){this.value=value;}
}
