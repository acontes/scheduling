/**
  PROJECT NAME:
    PAPI (ProActive Parallel Interface)
  FILE NAME:
    PAPI.java
  DEPENDENCY FILE(S):
    -

  DATE:
    01-28-04
  UPDATE:
    07-30-04
  REVISION NUMBER: 1AA-040730-00
  
  CHECKED:
    Yes

  LANGUAGE:
    C
  COMPILATION LINE:
    javac PAPI.java
    javah -jni PAPI -o PAPI.h
    javap -s -p PAPI > PAPI.sgn
  ENVIRONMENT CONFIGURATION:
    PATH= MPI include path

  DESCRIPTION:
    Accessibility of the MPICH implementation library within a JAVA class
      and to JAVA code from C/MPI code
    JAVA main file 
  REMARK(S):
    -
/**/


import org.objectweb.proactive.*;

public class PAPI
{
	public native void job();
	public native int  init();
	public native void finalize();
	public native void send( byte text[], int length, int receiver );
	public native void recv( byte text[], int length, int sender, int status );
	
	public static PAPI setup( String mpi_application_name )
	{
		PAPI     activation = (PAPI) null;
		int      initialisation_status = 0;
		
		System.out.println( "_ PAPI (Java side) > The interface is setting up" );
		try{ activation = (PAPI) ProActive.newActive( PAPI.class.getName(), 
							      new Object[]{ mpi_application_name } ); }
		catch( Exception e )
		{
			System.out.println( "! PAPI (Java side) > Asynchronous communications are not installed" );
			e.printStackTrace();
		}
		// Now, we proceed to the initialisation of the MPI environment
		// This Java thread must be synchronized with the MPI_Init function
		// In order to reach this goal, the PAPI_Init function is returning an int value
		//   so ProActive automatically synchonizes the two threads! It's tricky ;-) 
		System.out.println( "_ PAPI (Java side) > The MPI environment is currently being initialized" );
		initialisation_status = activation.init();
		if (initialisation_status == 1)
		{
			System.out.println( "_ PAPI (Java side) > The MPI environment was correctly initialized" );
		}
		else
		{
			System.out.println( "! PAPI (Java side) > The MPI environment failed to be initialized" );
		}
		
		System.out.println( "_ PAPI (Java side) > The interface is ready" );
		return( activation );
	}
	
	public PAPI() 
	{
	}
	
	public PAPI( String mpi_application_name )
	{
		System.out.println(  "_ PAPI (Java side) > The interface is loading the MPI application named "
		 		    + mpi_application_name );
		try{ System.loadLibrary( mpi_application_name ); }
		catch( Exception e )
		{
			System.out.println(  "! PAPI (Java side) > The MPI application named " 
			                    + mpi_application_name 
					    + " could not be loaded" );
			e.printStackTrace();
		}
		System.out.println(  "_ PAPI (Java side) > Loading of the MPI application named "
		 		    + mpi_application_name 
				    + " is finished" );

	}
}


/**
  END OF FILE
/**/