/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.slendy.updater;

import java.io.IOException;
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
    
    private final GitHub GH;
    GHRepository Repo;
    
    public Updater(String credenciales, String repository) throws IOException{
        GH = GitHubBuilder.fromPropertyFile(credenciales)
                .withRateLimitHandler(RateLimitHandler.FAIL)
                .build();
        Repo = GH.getRepository(repository);
    }
    
    public void ShowReleases() throws IOException{
        for (GHRelease R : Repo.listReleases()){
            System.out.println(R.getName()+" "+R.getTagName());
            for (GHAsset A : R.getAssets()) {
                System.out.println("  -> " + A.getName());
            }
        }
    }
    
    public void ShowFinalReleases() throws IOException{
        for (GHRelease R : Repo.listReleases()){
            if(R.isPrerelease() || R.isDraft()){continue;}
            System.out.println(R.getName()+" "+R.getTagName());
            for (GHAsset A : R.getAssets()) {
                System.out.println("  -> " + A.getName());
            }
        }
    }
    
}
