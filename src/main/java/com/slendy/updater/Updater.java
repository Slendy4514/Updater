/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.slendy.updater;

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
            try {
                U.setGH();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
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
            PM.SaveProp("version", ver);
//            PM.SaveProp("Ongoing",OnGoing);
            PM.SaveProp("login", login);
            PM.SaveProp("password", pass);
            PM.SaveProp("updateMode", "N");
            PM.SaveProp("name",currentName());
        }        
    }
    
    private void setGH() throws IOException{
        GH = GitHubBuilder.fromPropertyFile(PM.directory())
                .withRateLimitHandler(RateLimitHandler.FAIL)
                .build();
        Repo = GH.getRepository(repository);
    }
    
    public String currentVersion(){
        return CurrVer;
    }
    
    public String currentName(){
        return new File(c.getProtectionDomain()
                    .getCodeSource().getLocation().getPath()).getName();
    }
    
    public void ShowReleases() throws IOException{
        for (GHRelease R : Repo.listReleases()){
            System.out.println(R.getName()+" "+R.getTagName());
            for (GHAsset A : R.getAssets()) {
                System.out.println("  -> " + A.getName());
            }
        }
    }
    
    public ArrayList<String> getVersions() throws IOException{
        ArrayList<String> Tags = new ArrayList();
        for (GHRelease R : Repo.listReleases()){
            Tags.add(R.getTagName());
        }
        return Tags;
    }
    
    public String getLatest() throws IOException{
        return Repo.getLatestRelease().getTagName();
    }
    
    public void DownloadAssets(GHRelease R) throws IOException{
        for (GHAsset A : R.getAssets()) {
                URL asset = new URL(A.getBrowserDownloadUrl());
                FileUtils.copyURLToFile(asset, new File("New_"+currentName()));
            }
    }
    
    public void DownloadUpdate() throws IOException{
        DownloadAssets(Repo.getLatestRelease());
    }
    
    public boolean existsUpdate() throws IOException{
        return !Repo.getLatestRelease().getTagName().equals(this.CurrVer);
    }
    
    public boolean existsUpdateFor(String AltVer) throws IOException{
        return !Repo.getLatestRelease().getTagName().equals(AltVer);
    }
    
    public boolean Updating(){
        return !"N".equals(PM.ReadProp("updateMode"));
    }
}
