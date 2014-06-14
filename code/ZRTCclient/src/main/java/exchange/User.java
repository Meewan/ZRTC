package exchange;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Gauthier
 */
public class User {
    private String nickname;
    private String password;
    private String fonction;
    private String signature;
    
    public User(String nick, String passw, String fonct){
        this.nickname=nick;
        this.password=passw;
        this.fonction=fonct;
        this.signature="SIGNATURE";
    }
    
    public String getNick(){
        return this.nickname;
    }
    
    public String getSignature(){
        return this.signature;
    }
    
    public void setNick(String nickname){
        this.nickname=nickname;
    }
    
    public void setPass(String password){
        this.password=password;
    }
}
