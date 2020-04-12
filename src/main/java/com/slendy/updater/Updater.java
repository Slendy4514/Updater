/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.slendy.updater;

import static Basics.BThreads.Sleep;
import Files.PropManager;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import org.apache.commons.io.FileUtils;
import org.kohsuke.github.GHAsset;
import org.kohsuke.github.GHRelease;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.RateLimitHandler;

/**
 *
 * @author Matías
 */
public class Updater {
    
    private enum Data{
        UpMode("updateMode"),C_Login("login"),C_Pass("password"),UpVer("version"),ProgName("name");
        
        String text;
        
        private Data(String text){
            this.text = text;
        }
        
        @Override
        public String toString(){
            return text;
        }
    }
    
    public static class builder{
        //Obligatorios
        private final PropManager PM;
        private final Class c;
        private final String repository, login, pass;
        //Con Defaults
        private String ver = "Latest";
        boolean OnGoing = false;
        
        public builder(PropManager PM, String repository, String login, String pass, Class c){
            this.PM = PM;
            this.repository = repository;
            this.login = login;
            this.pass = pass;
            this.c = c;
        }
        
        public builder setVer(String ver){
            this.ver = ver;
            return this;
        }
        
        public builder SetOnGoing(){
            this.OnGoing = true;
            return this;
        }
        
        public Updater build(){
            
            Updater U = new Updater(PM,repository,login,pass,c);
            //Defaults
            U.ver = ver;
//            U.OnGoing = OnGoing;
            //Inicializar el Updater
            U.checkFile();
            U.setGH();
            return U;
        }
    }
    
    private GitHub GH;
    private GHRepository Repo;
    private final Class c;
    private final PropManager PM;
    private final String repository, login, pass, CurrVer;
    private String ver;
//    private boolean OnGoing;
    
    private Updater(PropManager PM, String repository, String login, String pass, Class c){
        this.PM = PM;
        this.repository = repository;
        this.login = login;
        this.pass = pass;  
        this.c = c;
        CurrVer = c.getPackage().getImplementationVersion();
    }
    
    private void checkFile(){
        if (!PM.exists()){
            PM.SaveProp(Data.UpVer+"", ver);
//            PM.SaveProp("Ongoing",OnGoing);
            PM.SaveProp(Data.C_Login+"", login);
            PM.SaveProp(Data.C_Pass+"", pass);
            PM.SaveProp(Data.UpMode+"", "N");
        }
        PM.SaveProp(Data.ProgName+"",currentName());        
    }
    
    private void setGH(){
        try{
            GH = GitHubBuilder.fromPropertyFile(PM.directory())
                    .withRateLimitHandler(RateLimitHandler.FAIL)
                    .build();
            Repo = GH.getRepository(repository);            
        }catch(IOException e){
            e.printStackTrace();
        }
    }
    
    public String currentVersion(){
        return CurrVer;
    }
    
    public String currentName(){
        return new File(c.getProtectionDomain()
                    .getCodeSource().getLocation().getPath()).getName();
    }
    
    public void ShowReleases(){
        try{
            for (GHRelease R : Repo.listReleases()){
                System.out.println(R.getName()+" "+R.getTagName());
                for (GHAsset A : R.getAssets()) {
                    System.out.println("  -> " + A.getName());
                }
            }
        }catch(IOException e){
            e.printStackTrace();
        }
    }
    
    public ArrayList<String> getVersions(){
        ArrayList<String> Tags = new ArrayList();
        try{
            for (GHRelease R : Repo.listReleases()){
                Tags.add(R.getTagName());
            }
        }catch(IOException e){
            e.printStackTrace();
        }
        return Tags;
    }
    
    public String getLatest(){
        try{
            return Repo.getLatestRelease().getTagName();
        }catch(IOException e){
            e.printStackTrace();
        }
        return null;
    }
    
    public void DownloadAssets(GHRelease R){
        try{
            for (GHAsset A : R.getAssets()) {
                URL asset = new URL(A.getBrowserDownloadUrl());
                if(A.getName().endsWith(".jar")){
                    FileUtils.copyURLToFile(asset, new File("New_"+currentName())); 
                }else{
                    FileUtils.copyURLToFile(asset, new File(A.getName()));
                    
                }
            }
        }catch(IOException e){
            e.printStackTrace();
        }
    }
    
    public void DownloadAssets(String Version){
        try {
            DownloadAssets(Repo.getReleaseByTagName(Version));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    public void DownloadUpdate(){
        try {
            DownloadAssets(Repo.getLatestRelease());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    public boolean existsUpdate(){
        try{
            return !Repo.getLatestRelease().getTagName().equals(this.CurrVer);
        }catch(IOException e){
            e.printStackTrace();
        }
        return false;
    }
    
    public boolean existsUpdateFor(String AltVer){
        try{
            return !Repo.getLatestRelease().getTagName().equals(AltVer);
        }catch(IOException e){
            e.printStackTrace();
        }
        return false;
    }
    
    public boolean Updating(){
        return !"N".equals(PM.ReadProp(Data.UpMode+""));
    }
    
    private void BeginUpdate(){
        try{
            this.DownloadAssets(Repo.getLatestRelease());
        }catch(IOException e){
            e.printStackTrace();
        }
        PM.SaveProp(Data.UpMode+"", "RN(w)");
        //Activar programa new
        PM.SaveProp(Data.UpMode+"", "RN");
        System.exit(0);
    }
    
    private void Rename(){
        while(PM.ReadProp(Data.UpMode+"").contains("w")){
            Sleep(1000);
        }
        String name = PM.ReadProp(Data.ProgName+"");
        File old = new File(name);
        //Hora de las pendejadas
        String razita = "razita";
        for (int i = 1; i < 7; i++){
            old.renameTo(new File(razita+".jar"));
            razita = razita+"a";
            Sleep(500);
        }
        Sleep(5000);
        //Fin de las pendejadas... borrar luego XD
        old.renameTo(new File("Old_"+name));
        PM.SaveProp(Data.UpMode+"", "R2(w)");
        //Activar programa old
        PM.SaveProp(Data.UpMode+"", "R2");
        System.exit(0);
    }
    
    private void Rename2(){
        while(PM.ReadProp(Data.UpMode+"").contains("w")){
            Sleep(1000);
        }
        String name = PM.ReadProp(Data.ProgName+"");
        File updated = new File("New_"+name);
        updated.renameTo(new File(name));
        PM.SaveProp(Data.UpMode+"", "Er(w)");
        //Activar programa new
        PM.SaveProp(Data.UpMode+"", "Er");
        System.exit(0);
        
    }
    
    private void Erase(){
        File old = new File("Old_"+Data.ProgName+"");
        old.delete();
        PM.SaveProp(Data.UpMode+"", "N");
    }
    
    public void Update(){
        switch(PM.ReadProp(Data.UpMode+"")){
            case "N":
                BeginUpdate();
                break;
            case "RN(w)":
                Rename();
                break;
            case "R2(w)":
                Rename2();
                break;
            case "Er(w)":
                Erase();
                break;
        }
    }
    
}
