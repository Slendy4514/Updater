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
        private PropManager PM;
        private Class c;
        private String repository, login, pass;
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
    private final String repository;
    private final String login;
    private final String pass;
    private String ver;
//    private boolean OnGoing;
    
    private Updater(PropManager PM, String repository, String login, String pass, Class c){
        this.PM = PM;
        this.repository = repository;
        this.login = login;
        this.pass = pass;  
        this.c = c;
    }
    
    private void checkFile(){
        if (!PM.exists()){
            PM.SaveProp("version", ver);
//            PM.SaveProp("Ongoing",OnGoing);
            PM.SaveProp("login", login);
            PM.SaveProp("password", pass);
            PM.SaveProp("updateMode", "0");
            PM.SaveProp("name", new File(c.getProtectionDomain()
                    .getCodeSource().getLocation().getPath()).getName());
        }        
    }
    
    private void setGH() throws IOException{
        GH = GitHubBuilder.fromPropertyFile(PM.directory())
                .withRateLimitHandler(RateLimitHandler.FAIL)
                .build();
        Repo = GH.getRepository(repository);
    }
    
    public static String currentVersion(Class c){
        return new File(c.getProtectionDomain()
                    .getCodeSource().getLocation().getPath()).getName();
    }
    
    public void ShowReleases(boolean all) throws IOException{
        for (GHRelease R : Repo.listReleases()){
            if(all && (R.isPrerelease() || R.isDraft())){continue;}
            System.out.println(R.getName()+" "+R.getTagName());
            for (GHAsset A : R.getAssets()) {
                System.out.println("  -> " + A.getName());
            }
        }
    }
    
    public void ShowReleases() throws IOException{
        this.ShowReleases(false);
    }
    
    public ArrayList<String> getVersions(boolean all) throws IOException{
        ArrayList<String> Tags = new ArrayList();
        for (GHRelease R : Repo.listReleases()){
            if(all && (R.isPrerelease() || R.isDraft())){continue;}
            Tags.add(R.getTagName());
        }
        return Tags;
    }
    
    public ArrayList<String> getVersions() throws IOException{
        return getVersions(false);
    }
    
    public GHRelease getLatest(boolean  stable) throws IOException{
        if(!stable){
            return Repo.getLatestRelease();
        }else{
            ArrayList<String> tags = getVersions();
            return Repo.getReleaseByTagName(tags.get(tags.size()-1)); //revisar si es ultima o primera
        }
    }
    
    public GHRelease getLatest() throws IOException{
        return getLatest(true);
    }
    
    public void DownloadAssets(GHRelease R) throws IOException{
        for (GHAsset A : R.getAssets()) {
                URL asset = new URL(A.getBrowserDownloadUrl());
                FileUtils.copyURLToFile(asset, new File(A.getName()));
            }
    }
    
    public void DownloadUpdate() throws IOException{
        DownloadAssets(getLatest());
    }
    
    public boolean checkForUpdate(){
        return true;
    }
    
    public boolean Updating(){
        return !"N".equals(PM.ReadProp("updateMode"));
    }
}
