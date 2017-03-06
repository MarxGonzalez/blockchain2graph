package com.oakinvest.b2g.batch;

import com.oakinvest.b2g.service.StatusService;
import org.neo4j.ogm.session.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Temporary batch used because multi threading doesn't work.
 * Created by straumat on 02/03/17.
 */
@Component
public class Batch {

	/**
	 * Pause between imports.
	 */
	private static final int PAUSE_BETWEEN_IMPORTS = 100;

	/**
	 * How many milli seconds in one second.
	 */
	private static final float MILLISECONDS_IN_SECONDS = 1000F;

	/**
	 * Logger.
	 */
	private final Logger log = LoggerFactory.getLogger(Batch.class);

	/**
	 * Maximum block import duration.
	 */
	@Value("${spring.data.neo4j.block-import.maximum-duration}")
	private int maximumBlockImportDuration;

	/**
	 * Import batch.
	 */
	@Autowired
	private BitcoinImportBatchBlocks batchBlocks;

	/**
	 * Import batch.
	 */
	@Autowired
	private BitcoinImportBatchAddresses batchAddresses;

	/**
	 * Import batch.
	 */
	@Autowired
	private BitcoinImportBatchTransactions batchTransactions;

	/**
	 * Import batch.
	 */
	@Autowired
	private BitcoinImportBatchRelations batchRelations;

	/**
	 * Status service.
	 */
	@Autowired
	private StatusService status;

	/**
	 * Neo4j session.
	 */
	@Autowired
	private Session session;

	/**
	 * Import data.
	 */
	@Scheduled(fixedDelay = PAUSE_BETWEEN_IMPORTS)
	@SuppressWarnings("checkstyle:designforextension")
	public void importData() {
		final long start = System.currentTimeMillis();

		// Importing the block.
		try {
			batchBlocks.importData();
			batchAddresses.importData();
			batchTransactions.importData();
			batchRelations.importData();
		} catch (Exception e) {
			status.addError("Error in the batch process : " + e.getMessage());
		}

		// Adding a statistic.
		final float elapsedTime = (System.currentTimeMillis() - start) / MILLISECONDS_IN_SECONDS;
		status.addBlockImportDurationStatistic(elapsedTime);

		// If it takes too much time, we clear the neo4j session.
		if (elapsedTime > maximumBlockImportDuration) {
			log.info("Clearing the neo4j session");
			session.clear();
		}
	}

}
