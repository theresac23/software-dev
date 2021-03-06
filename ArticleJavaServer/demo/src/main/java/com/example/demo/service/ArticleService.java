package com.example.demo.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.json.JSONObject;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.controller.ArticleController;
import com.example.demo.entities.ArticleEntity;
import com.example.demo.entities.StatusEntity;
import com.example.demo.entities.ArticleHasStatusEntity;
import com.example.demo.repository.ArticleHasStatusRepository;
import com.example.demo.repository.ArticleRepository;
import com.example.demo.repository.StatusRepository;

import com.example.demo.service.BuzzService;
import com.example.demo.service.FileService;

@Service
public class ArticleService {
	private static org.slf4j.Logger logger = LoggerFactory.getLogger(ArticleController.class);
	
	@Autowired private ArticleRepository articleRepository;
	@Autowired private StatusRepository statusRepository;
	@Autowired private ArticleHasStatusRepository articleHasStatusRepository;
	@Autowired private BuzzService buzzService;
	@Autowired private FileService fileService;
	@Autowired private ScrapeService scrapeService;

	
	public ArticleEntity findArticleById(Integer id) {
		Optional<ArticleEntity> a = articleRepository.findById(id);
		if (a.isPresent())
			return a.get();
		else
			return null;
	}
	
	public List<ArticleEntity> findArticleByStatus(String statusCode) {
		return articleRepository.findByStatusCode(statusCode);
	}

	public ArticleEntity findArticleByUrl(String url) {
		Optional<ArticleEntity> a = articleRepository.findByUrl(url);
		if (a.isPresent()) {
			return a.get();
		} else {
			return null;
		}
	}
	
	public ArticleEntity processSubmitArticle(String url) {
		// create new record
		ArticleEntity newArticle = createNewArticle(url, "USER");
		
		//see if it's in buzzsumo
		JSONObject jArticle = buzzService.getBuzz(url);
		
		//update with buzz fields
		ArticleEntity updatedArticle = updateArticleWithBuzz(jArticle, newArticle);
		
		//scrape article, 
		String articleText = scrapeService.scrapeArticle(url);
		updatedArticle.setArticleText(articleText);
		articleRepository.save(updatedArticle);
		
		// sha256, create metadata, tar.gz
		fileService.makeFile(updatedArticle);	
		
		return updatedArticle;

	}
	public ArticleEntity createNewArticle(String url, String status) {
		ArticleEntity a = new ArticleEntity();
		a.setUrl(url);
		ArticleEntity b = articleRepository.save(a);
		ArticleHasStatusEntity ahs = new ArticleHasStatusEntity();
		ahs.setArticleId(b.getId());
		Optional<StatusEntity> s = statusRepository.findByStatusCode(status);
		if (s.isPresent()) {
			ahs.setArticleStatusId(s.get().getId());
		} else {
			ahs.setArticleStatusId(1);
		}
		ArticleHasStatusEntity newAhs = articleHasStatusRepository.save(ahs);
		Optional<ArticleEntity> c = articleRepository.findById(b.getId());
		if (c.isPresent()) {
			return c.get();
		} else {
			return null;
		}
	}

	public List<ArticleEntity> findArticleByTitle(String title) {
		return articleRepository.findByTitle(title);
	}
	
	public List<ArticleEntity> findAllArticles() {
		Iterable<ArticleEntity> articleIterable = articleRepository.findAll();
		List<ArticleEntity> list = new ArrayList<ArticleEntity>();
		articleIterable.forEach(list::add);
		return list;
		
	}
	
	public ArticleEntity updateStatus(Integer id, String status, String comment) {
		Optional<ArticleEntity> articleToFind = articleRepository.findById(id);
		
		ArticleEntity foundArticle;
		if (articleToFind.isPresent()) 
			foundArticle = articleToFind.get();
		else
			return null;
			
		Optional<StatusEntity> dbStatusToFind = statusRepository.findByStatusCode(status);
		
		if (dbStatusToFind.isPresent()) {
			ArticleHasStatusEntity newStatus = new ArticleHasStatusEntity();
			StatusEntity dbStatus = dbStatusToFind.get();
			newStatus.setArticleId(foundArticle.getId());
			newStatus.setArticleStatusId(dbStatus.getId());
			newStatus.setDateChanged(new Date());
			newStatus.setComment(comment);
			articleHasStatusRepository.save(newStatus);
			Optional<ArticleEntity> updatedArticleToFind = articleRepository.findById(foundArticle.getId());
			if (updatedArticleToFind.isPresent()) {
				ArticleEntity foundUpdatedArticle = updatedArticleToFind.get();
				return foundUpdatedArticle;
			}
		}
		return null;			
	}
	
	public ArticleEntity updateArticleWithBuzz(JSONObject jArticle, ArticleEntity article) {
		article.setAlexaRank(jArticle.optInt("alexa_rank"));
		article.setAngryCount(jArticle.optInt("angry_count"));
//		article.setArticleAmplifiers(articleAmplifiers);
		article.setArticleTitle(jArticle.optString("title"));
		article.setAuthor(jArticle.optString("author_name"));
		article.setBuzzsumoArticleId(jArticle.optInt("id"));
		article.setDomainName(jArticle.optString("domain_name"));
		article.setEvergreenScore(jArticle.optDouble("evergreen_score"));
		article.setFacebookComments(jArticle.optInt("facebook_comments"));
		article.setFacebookLikes(jArticle.optInt("facebook_likes"));
		article.setFacebookShares(jArticle.optInt("total_facebook_shares"));
		article.setHahaCount(jArticle.optInt("haha_count"));
		article.setLoveCount(jArticle.optInt("love_count"));
		article.setNumLinkingDomains(jArticle.optInt("num_linking_domains"));
//		article.setPublishDate(new Date((jArticle.optInt("published_date") * 1000)));
		article.setSadCount(jArticle.optInt("sad_count"));
		article.setTotalRedditEngagements(jArticle.optInt("total_reddit_engagements"));
		article.setTotalShares(jArticle.optInt("total_shares"));
		article.setTwitterShares(jArticle.optInt("twitter_shares"));
//		article.setUpdatedAt(updatedAt);
		article.setWowCount(jArticle.optInt("wow_count"));
		
		articleRepository.save(article);
		
		return article;
	}
	
	public ArticleEntity updateArticle(Integer id, ArticleEntity article, String comment) {
		if (article.getId().equals(id)) {
			articleRepository.save(article);
			return article;
		} else {
			return null;
		}
	}

	public ArticleEntity updateVizData(String sha, String visData, String comment) {
		ArticleEntity article = articleRepository.findOneByArticleHash(sha);
		
		if (article != null) {
			article.setVisData(visData);
			articleRepository.save(article);
			return article;
		} else {
			return null;
		}
	}


}
