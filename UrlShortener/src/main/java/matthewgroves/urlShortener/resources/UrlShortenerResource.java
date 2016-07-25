package matthewgroves.urlShortener.resources;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.codahale.metrics.annotation.Timed;

import matthewgroves.urlShortener.api.ShortenedUrl;
import matthewgroves.urlShortener.db.UrlDAO;

/**
 * The /shrtn resource class
 * 
 * @author Matthew Groves <matthew.b.groves@gmail.com>
 *
 */
@Path("shrtn")
public class UrlShortenerResource {
	private UrlDAO dao;
	
	public UrlShortenerResource(UrlDAO dao) {
		this.dao = dao;
		
		this.dao.createUrlsTableIfNeeded();
	}
	
	/**
	 * Redirects to the full URL for the desired shortened URL {@link id}
	 * 
	 * @param id
	 *            - the id argument to lookup in the DB and expand to a full URL
	 * @return Redirects user to expanded URL, if it exists. Otherwise, sends a
	 *         404 status. If an exception occurs, a 500 status is sent with the
	 *         exception in the response body.
	 * @throws URISyntaxException 
	 * @throws IOException
	 */
	@GET
	@Timed
	@Path("{id}")
	public Response redirectUrl(@PathParam("id") String id) throws URISyntaxException {
		ShortenedUrl sUrl = expandUrl(id);
		URI redirectTo = new URI(sUrl.getFullUrl());
		return Response.seeOther(redirectTo).build();
	}
	
	/**
	 * Gets the full URL for the desired shortened URL {@link id}
	 * 
	 * @param id
	 *            - the id argument to lookup in the DB and expand to a full URL
	 * @return Gets the expanded URL, if it exists. Otherwise, sends a 404
	 *         status. If an exception occurs, a 500 status is sent with the
	 *         exception in the response body.
	 * @throws IOException
	 */
	@GET
	@Timed
	@Path("{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public ShortenedUrl getExpandedUrl(@PathParam("id") String id) {
		return expandUrl(id);
	}
	
	/**
	 * Helper function
	 * 
	 * @param id
	 *            - the id argument to lookup in the DB and expand to a full URL
	 * @return The expanded URI for the specified {@link id}
	 */
	private ShortenedUrl expandUrl(String id) {
		// TODO - Why this shouldn't be done: http://12factor.net
		// TODO - Enhancements should be entered as "Feature Requests" on GitHub
		// TODO ENHANCEMENT - log this expansion into a new table in the DB
		// (date/time, user's IP, other info from request?)
		try {
			// The id portion of a shortened URL is really just the base-36
			// encoded version of its id in the database
			long idVal = Long.parseLong(id, 36);
			String url = dao.findUrlById(idVal);
			if (url != null) {
				ShortenedUrl sUrl = new ShortenedUrl(idVal, url,
						"http://localhost:8080/shrtn/" + Long.toString(idVal, 36));
				return sUrl;
			}
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}
		
		throw new NotFoundException();
	}
	
	/**
	 * Creates a new entry in the DB for the given {@link fullUrl}
	 * 
	 * @param fullUrl
	 *            - the full URL the user wishes to shorten
	 * @return If success, this returns a {@link ShortenedUrl} record.
	 *         Otherwise, an HTTP 500 status is returned with the exception in
	 *         the response body.
	 */
	@POST
	@Timed
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.APPLICATION_JSON)
	public ShortenedUrl addShortenedUrl(String fullUrl) {
		if (fullUrl != null && fullUrl.length() > 0 && fullUrl.length() < 2048) {
			long id = dao.insertUrl(fullUrl);
			ShortenedUrl sUrl = new ShortenedUrl(id, fullUrl, "http://localhost:8080/shrtn/" + Long.toString(id, 36));
			return sUrl;
		}
		
		throw new WebApplicationException("Invalid fullUrl");
	}
}