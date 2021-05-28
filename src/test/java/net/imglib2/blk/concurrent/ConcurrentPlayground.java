package net.imglib2.blk.concurrent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;

public class ConcurrentPlayground
{
	public static void main( String[] args ) throws ExecutionException, InterruptedException
	{
		final List< CompletableFuture< String > > futures = new ArrayList<>();


		for ( int i = 0; i < 100; ++i )
		{
			final int j = i;
			final CompletableFuture< String > f = CompletableFuture.supplyAsync( () -> {
				try
				{
					Thread.sleep( 100 );

					final List< CompletableFuture< Integer > > futures1 = new ArrayList<>();
					for ( int k = 0; k < 100; k++ )
					{
						final int l = k;
						final CompletableFuture< Integer > f1 = CompletableFuture.supplyAsync( () -> l );
						futures1.add( f1 );
					}
					final int sum = futures1.stream().mapToInt( f2 -> f2.join().intValue() ).sum();
					return j + " : " + Thread.currentThread().getName() + " : " + sum;
				}
				catch ( InterruptedException e )
				{
					throw new CompletionException( e );
				}
			} );
			futures.add( f );
		}

		for ( CompletableFuture< String > future : futures )
		{
			System.out.println( future.get() );
		}
	}

	public static void main1( String[] args ) throws ExecutionException, InterruptedException
	{
		final List< CompletableFuture< String > > futures = new ArrayList<>();

		System.out.println( "activeThreadCount = " + ForkJoinPool.commonPool().getActiveThreadCount() );
		System.out.println( "runningThreadCount = " + ForkJoinPool.commonPool().getRunningThreadCount() );

		for ( int i = 0; i < 100; ++i )
		{
			final int j = i;
			final CompletableFuture< String > f = CompletableFuture.supplyAsync( () -> {
				try
				{
					Thread.sleep( 100 );
					return j + " : " + Thread.currentThread().getName();
				}
				catch ( InterruptedException e )
				{
					throw new CompletionException( e );
				}
			} );
			futures.add( f );
		}

		System.out.println( "activeThreadCount = " + ForkJoinPool.commonPool().getActiveThreadCount() );
		System.out.println( "runningThreadCount = " + ForkJoinPool.commonPool().getRunningThreadCount() );

		for ( CompletableFuture< String > future : futures )
		{
			System.out.println( future.get() );
		}
	}



}
