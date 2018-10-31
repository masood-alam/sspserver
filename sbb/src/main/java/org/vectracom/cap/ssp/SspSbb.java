package org.vectracom.cap.ssp;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.slee.ActivityContextInterface;
import javax.slee.CreateException;
import javax.slee.EventContext;
import javax.slee.RolledBackContext;
import javax.slee.Sbb;
import javax.slee.SbbContext;
import javax.slee.facilities.Tracer;
import javax.slee.facilities.TimerFacility;

import javax.slee.resource.ResourceAdaptorTypeID;

import org.mobicents.protocols.ss7.cap.api.CAPApplicationContext;
import org.mobicents.protocols.ss7.cap.api.CAPException;
import org.mobicents.protocols.ss7.cap.api.CAPParameterFactory;
import org.mobicents.protocols.ss7.cap.api.CAPProvider;
import org.mobicents.protocols.ss7.cap.api.EsiBcsm.OAbandonSpecificInfo;
import org.mobicents.protocols.ss7.cap.api.EsiBcsm.OAnswerSpecificInfo;
import org.mobicents.protocols.ss7.cap.api.EsiBcsm.OCalledPartyBusySpecificInfo;
import org.mobicents.protocols.ss7.cap.api.EsiBcsm.ODisconnectSpecificInfo;
import org.mobicents.protocols.ss7.cap.api.EsiBcsm.ONoAnswerSpecificInfo;
import org.mobicents.protocols.ss7.cap.api.EsiBcsm.RouteSelectFailureSpecificInfo;
import org.mobicents.protocols.ss7.cap.api.isup.CalledPartyNumberCap;
import org.mobicents.protocols.ss7.cap.api.isup.CallingPartyNumberCap;
import org.mobicents.protocols.ss7.cap.api.isup.CauseCap;
import org.mobicents.protocols.ss7.cap.api.isup.LocationNumberCap;
import org.mobicents.protocols.ss7.cap.api.primitives.BCSMEvent;
import org.mobicents.protocols.ss7.cap.api.primitives.EventTypeBCSM;
import org.mobicents.protocols.ss7.cap.api.primitives.ReceivingSideID;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.CAPDialogCircuitSwitchedCall;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.RequestReportBCSMEventRequest;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.primitive.DestinationRoutingAddress;
import org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.primitive.EventSpecificInformationBCSM;
import org.mobicents.protocols.ss7.cap.service.circuitSwitchedCall.InitialDPRequestImpl;
import org.mobicents.protocols.ss7.inap.api.primitives.LegType;
import org.mobicents.protocols.ss7.inap.api.primitives.MiscCallInfo;
import org.mobicents.protocols.ss7.inap.api.primitives.MiscCallInfoMessageType;
import org.mobicents.protocols.ss7.indicator.NatureOfAddress;
import org.mobicents.protocols.ss7.indicator.RoutingIndicator;
import org.mobicents.protocols.ss7.isup.message.parameter.CalledPartyNumber;
import org.mobicents.protocols.ss7.isup.message.parameter.CallingPartyNumber;
import org.mobicents.protocols.ss7.isup.message.parameter.CauseIndicators;
import org.mobicents.protocols.ss7.isup.message.parameter.LocationNumber;
import org.mobicents.protocols.ss7.isup.message.parameter.NAINumber;
import org.mobicents.protocols.ss7.map.api.primitives.AddressNature;
import org.mobicents.protocols.ss7.map.api.primitives.ISDNAddressString;
import org.mobicents.protocols.ss7.map.api.primitives.NumberingPlan;
import org.mobicents.protocols.ss7.map.api.service.mobility.subscriberInformation.LocationInformation;
import org.mobicents.protocols.ss7.sccp.SccpListener;
import org.mobicents.protocols.ss7.sccp.impl.parameter.BCDEvenEncodingScheme;
import org.mobicents.protocols.ss7.sccp.impl.parameter.ParameterFactoryImpl;
import org.mobicents.protocols.ss7.sccp.impl.parameter.SccpAddressImpl;
import org.mobicents.protocols.ss7.sccp.parameter.EncodingScheme;
import org.mobicents.protocols.ss7.sccp.parameter.GlobalTitle;
import org.mobicents.protocols.ss7.sccp.impl.parameter.GlobalTitle0100Impl;
import org.mobicents.protocols.ss7.sccp.parameter.ParameterFactory;
import org.mobicents.protocols.ss7.sccp.parameter.SccpAddress;

import org.mobicents.slee.SbbContextExt;
import org.mobicents.slee.resource.cap.CAPContextInterfaceFactory;
import org.restcomm.camelgateway.EventsSerializeFactory;
import org.restcomm.camelgateway.XmlCAPDialog;

//import org.restcomm.camelgateway.EventsSerializeFactory;
//import org.restcomm.camelgateway.XmlCAPDialog;

import net.java.slee.resource.http.HttpSessionActivity;
import net.java.slee.resource.http.events.HttpServletRequestEvent;


import org.jboss.mx.util.MBeanServerLocator;



public abstract class SspSbb implements Sbb {
	private SbbContextExt sbbContext; // This SBB's SbbContext

	private Tracer logger;

	private CAPContextInterfaceFactory capAcif;
	protected CAPProvider capProvider;
	protected static final ResourceAdaptorTypeID capRATypeID = new ResourceAdaptorTypeID("CAPResourceAdaptorType",
             "org.mobicents", "2.0");
	protected static final String capRaLink = "CAPRA";

	protected EventsSerializeFactory eventsSerializeFactory = null;
	protected CAPParameterFactory capParameterFactory;
	protected ParameterFactory sccpParameterFact;
	protected TimerFacility timerFacility = null;

	 private CAPDialogCircuitSwitchedCall currentCapDialog;
	 private CallContent cc;
	 ArrayList<BCSMEvent> bcsmEventList = new ArrayList<BCSMEvent>();	

	 
	public void onServiceStartedEvent(javax.slee.serviceactivity.ServiceStartedEvent event, ActivityContextInterface aci/*, EventContext eventContext*/) {
		logger.info("SSP Service started");
	}


	protected EventsSerializeFactory getEventsSerializeFactory() {
		if (this.eventsSerializeFactory == null) {
			this.eventsSerializeFactory = new EventsSerializeFactory();
		}
		return this.eventsSerializeFactory;
	}


	// TODO: Perform further operations if required in these methods.
	public void setSbbContext(SbbContext context) { 
		this.sbbContext = (SbbContextExt) context;
		 this.logger = sbbContext.getTracer(SspSbb.class.getSimpleName());
		  try {
              this.capAcif = (CAPContextInterfaceFactory) this.sbbContext.getActivityContextInterfaceFactory(capRATypeID);
              this.capProvider = (CAPProvider) this.sbbContext.getResourceAdaptorInterface(capRATypeID, capRaLink);
              this.capParameterFactory = this.capProvider.getCAPParameterFactory();
              this.sccpParameterFact = new ParameterFactoryImpl();
    		  logger.info("SBB context successfully set");
    		  
      } catch (Exception ne) {
              logger.severe("Could not set SBB context:", ne);
      }
	}
	public void unsetSbbContext() { 
		this.sbbContext = null;
		this.capAcif = null;
		this.capProvider = null;
		this.capParameterFactory = null;
		this.logger = null;
	}

	// TODO: Implement the lifecycle methods if required
	public void sbbCreate() throws javax.slee.CreateException {}
	public void sbbPostCreate() throws javax.slee.CreateException {}
	public void sbbActivate() {}
	public void sbbPassivate() {}
	public void sbbRemove() {}
	public void sbbLoad() {}
	public void sbbStore() {}
	public void sbbExceptionThrown(Exception exception, Object event, ActivityContextInterface activity) {}
	public void sbbRolledBack(RolledBackContext context) {}
	

	
	/**
	 * Convenience method to retrieve the SbbContext object stored in setSbbContext.
	 * 
	 * TODO: If your SBB doesn't require the SbbContext object you may remove this 
	 * method, the sbbContext variable and the variable assignment in setSbbContext().
	 *
	 * @return this SBB's SbbContext object
	 */
	
	protected SbbContextExt getSbbContext() {
		return sbbContext;
	}

	
	public void onDIALOG_ACCEPT(org.mobicents.slee.resource.cap.events.DialogAccept event, 
			ActivityContextInterface aci) {
		}



		public void onDIALOG_CLOSE(org.mobicents.slee.resource.cap.events.DialogClose event, 
			ActivityContextInterface aci) {

			logger.info("onDialogClose");

			/*        
			XmlCAPDialog dialog = this.getXmlCAPDialog();
	        	dialog.setTCAPMessageType(event.getCAPDialog().getTCAPMessageType());

	        	try {
	            		byte[] data = this.getEventsSerializeFactory().serialize(dialog);
	            		this.sendXmlPayload(data);
	        	} catch (Exception e) {
	            		logger.severe(String.format("Exception while sending onDIALOG_CLOSE to an application", event), e);
	            		this.endHttpClientActivity();
	            		return;
	        	}
	        	dialog.reset();
	        	this.setXmlCAPDialog(dialog);
	        	this.setCapDialogClosed(true);
	        	this.updateDialogTime();
			*/

		}

	    public void onDIALOG_DELIMITER(org.mobicents.slee.resource.cap.events.DialogDelimiter event, 
			ActivityContextInterface aci) {
	    
			logger.info("onDIALOG_DELIMTER");
			 if (this.currentCapDialog != null && this.cc != null && this.cc.step != Step.disconnected) {
		            if (this.cc.activityTestInvokeId != null) {
		                try {
		                    currentCapDialog.addActivityTestResponse(this.cc.activityTestInvokeId);
		                    this.cc.activityTestInvokeId = null;
		                    currentCapDialog.send();
		                } catch (CAPException e) {
		                    // TODO Auto-generated catch block
		                    e.printStackTrace();
		                }
		            }
		        }



			/*    
			XmlCAPDialog dialog = this.getXmlCAPDialog();
	        	dialog.setTCAPMessageType(event.getCAPDialog().getTCAPMessageType());
	        	this.resetTimer(aci);
	        
	        	try {
	            		byte[] data = this.getEventsSerializeFactory().serialize(dialog);
	            		this.sendXmlPayload(data);
	        	} catch (Exception e) {
	            		logger.severe(String.format("Exception while sending onDIALOG_DELIMITER to an application", 
				event), e);
	            		// TODO Abort DIalog?
	        	}
	        	dialog.reset();
	        	this.setXmlCAPDialog(dialog);
			*/
	    }


		public void onDIALOG_NOTICE(org.mobicents.slee.resource.cap.events.DialogNotice event, ActivityContextInterface aci) {
			logger.warning(String.format("onDIALOG_NOTICE for Dialog=%s", event.getCAPDialog()));
		}


		public void onDIALOG_PROVIDERABORT(org.mobicents.slee.resource.cap.events.DialogProviderAbort event,
				ActivityContextInterface aci) {

			logger.warning(String.format("onDIALOG_PROVIDERABORT for Dialog=%s", event.getCAPDialog()));

		/*
	        XmlCAPDialog dialog = this.getXmlCAPDialog();
	        dialog.reset();
	        dialog.setPAbortCauseType(event.getPAbortCauseType());
	        dialog.setTCAPMessageType(event.getCAPDialog().getTCAPMessageType());

	        try {
	            byte[] data = this.getEventsSerializeFactory().serialize(dialog);
	            this.sendXmlPayload(data);
	        } catch (Exception e) {
	            logger.severe(String.format("Exception while sending onDIALOG_PROVIDERABORT to an application", event), e);
	            this.endHttpClientActivity();
	        }

	        this.setCapDialogClosed(true);
			*/

		}


		public void onDIALOG_RELEASE(org.mobicents.slee.resource.cap.events.DialogRelease event,
				ActivityContextInterface aci) {

			logger.info("onDIALOG_RELEASE");
			this.currentCapDialog = null;
			//this.cc = null;

			//just to be safe
			//this.cancelTimer();
		}

		public void onINITIAL_DP_REQUEST(
				org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.InitialDPRequest event,
				ActivityContextInterface aci) {

		
			/*
			XmlCAPDialog dialog = this.getXmlCAPDialog();
			dialog.addCAPMessage(((CAPEvent) event).getWrappedEvent());
			this.setXmlCAPDialog(dialog);

	        	camelStatAggregator.updateMessagesRecieved();
	        	camelStatAggregator.updateMessagesAll();
			*/
		}


		public void onINITIATE_CALL_ATTEMPT_REQUEST(
			org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.InitiateCallAttemptRequest event,
				ActivityContextInterface aci) {
		}


		public void onINITIATE_CALL_ATTEMPT_RESPONSE(
			org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.InitiateCallAttemptResponse event,
				ActivityContextInterface aci) {
		}


		public void onMOVE_LEG_REQUEST(
			org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.MoveLegRequest event, 
				ActivityContextInterface aci) {
		}

		public void onMOVE_LEG_RESPONSE(
			org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.MoveLegResponse event, 
				ActivityContextInterface aci) {
		}


		public void onACTIVITY_TEST_REQUEST(
				org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.ActivityTestRequest event,
				ActivityContextInterface aci) {
			logger.info("onACTIVITY_TEST_REQUEST");
		 if (this.currentCapDialog != null && this.cc != null && this.cc.step != Step.disconnected) {
		            this.cc.activityTestInvokeId = event.getInvokeId();
		        }

		}

		public void onACTIVITY_TEST_RESPONSE(
				org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.ActivityTestResponse event,
				ActivityContextInterface aci) {
		}


		public void onAPPLY_CHARGING_REPORT_REQUEST(
				org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.ApplyChargingReportRequest event,
				ActivityContextInterface aci) {
		/*
	        XmlCAPDialog dialog = this.getXmlCAPDialog();
	        dialog.addCAPMessage(((CAPEvent) event).getWrappedEvent());
	        this.setXmlCAPDialog(dialog);

	        camelStatAggregator.updateMessagesRecieved();
	        camelStatAggregator.updateMessagesAll();
		*/

		}

		public void onAPPLY_CHARGING_REQUEST(
				org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.ApplyChargingRequest event,
				ActivityContextInterface aci) {
		}


		public void onDIALOG_REQUEST(org.mobicents.slee.resource.cap.events.DialogRequest event,
				ActivityContextInterface aci/* , EventContext eventContext */) {


			logger.info("onDIALOG_REQUEST");
			/*
			CAPDialog capDialog = event.getCAPDialog();
			XmlCAPDialog dialog = new XmlCAPDialog(capDialog.getApplicationContext(), capDialog.getLocalAddress(),
					capDialog.getRemoteAddress(), capDialog.getLocalDialogId(), capDialog.getRemoteDialogId());
			int networkId = capDialog.getNetworkId();
			dialog.setNetworkId(networkId);
			this.setXmlCAPDialog(dialog);

			NetworkRoutingRule networkRoutingRule = networkRoutingRuleManagement.getNetworkRoutingRule(networkId);

			if (networkRoutingRule == null) {
				String route = camlePropertiesManagement.getRoute();
				if (logger.isFineEnabled()) {
					logger.fine("No NetworkRoutingRule configured for network-id " + networkId + " Using the default one "
							+ route);
				}
				networkRoutingRule = new NetworkRoutingRule();
				networkRoutingRule.setRuleUrl(route);
			}

			this.setNetworkRoutingRule(networkRoutingRule);

			if (networkRoutingRule.getRuleUrl() == null) {
				logger.warning("No routing rule defined for networkId " + networkId
						+ " Disconnecting from ACI, no new messages will be processed");
				aci.detach(this.sbbContext.getSbbLocalObject());

			}

	        camelStatAggregator.updateDialogsAllEstablished();
	        this.setStartDialogTime(System.currentTimeMillis());
			*/
		}


		public void onDIALOG_TIMEOUT(org.mobicents.slee.resource.cap.events.DialogTimeout event,
				ActivityContextInterface aci) {
//	        if (logger.isFineEnabled()) {
	            logger.info(String.format("onDIALOG_TIMEOUT for Dialog=%s Calling keepAlive", event.getCAPDialog()));
	        

			event.getCAPDialog().keepAlive();
		}


		public void onDIALOG_USERABORT(org.mobicents.slee.resource.cap.events.DialogUserAbort event,
				ActivityContextInterface aci) {
			logger.warning(String.format("onDIALOG_USERABORT for Dialog=%s", event.getCAPDialog()));

		/*
	        XmlCAPDialog dialog = this.getXmlCAPDialog();
	        dialog.reset();
	        try {
	            dialog.abort(event.getUserReason());
	        } catch (CAPException e1) {
	            // This can not occur
	            e1.printStackTrace();
	        }
	        dialog.setTCAPMessageType(event.getCAPDialog().getTCAPMessageType());

	        try {
	            byte[] data = this.getEventsSerializeFactory().serialize(dialog);
	            this.sendXmlPayload(data);
	        } catch (Exception e) {
	            logger.severe(String.format("Exception while sending onDIALOG_USERABORT to an application", event), e);
	            this.endHttpClientActivity();
	        }

	        this.setCapDialogClosed(true);
		*/

		}


		public void onASSIST_REQUEST_INSTRUCTIONS_REQUEST(
				org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.AssistRequestInstructionsRequest event,
				ActivityContextInterface aci) {
		}

		public void onCALL_INFORMATION_REPORT_REQUEST(
				org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.CallInformationReportRequest event,
				ActivityContextInterface aci) {
		}

		public void onCALL_INFORMATION_REQUEST_REQUEST(
				org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.CallInformationRequestRequest event,
				ActivityContextInterface aci/* , EventContext eventContext */) {
		}

		public void onCANCEL_REQUEST(org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.CancelRequest event,
				ActivityContextInterface aci/* , EventContext eventContext */) {
		}

		public void onCONNECT_REQUEST(org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.ConnectRequest event,
				ActivityContextInterface aci/* , EventContext eventContext */) {
		}

		public void onCONNECT_TO_RESOURCE_REQUEST(
				org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.ConnectToResourceRequest event,
				ActivityContextInterface aci/* , EventContext eventContext */) {
		}

		public void onCONTINUE_REQUEST(
				org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.ContinueRequest event,
				ActivityContextInterface aci) {
		  logger.info("onCONTINUE_REQUEST");
		if (this.cc != null)
				this.cc.step = Step.callAllowed;
			else
				logger.info("cc = null\r\n\r\n");
	        event.getCAPDialog().processInvokeWithoutAnswer(event.getInvokeId());
	    
		}

		public void onDISCONNECT_FORWARD_CONNECTION_REQUEST(
				org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.DisconnectForwardConnectionRequest event,
				ActivityContextInterface aci/* , EventContext eventContext */) {
		}

		public void onESTABLISH_TEMPORARY_CONNECTION_REQUEST(
				org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.EstablishTemporaryConnectionRequest event,
				ActivityContextInterface aci/* , EventContext eventContext */) {
		}


		public void onEVENT_REPORT_BCSM_REQUEST(
				org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.EventReportBCSMRequest event,
				ActivityContextInterface aci/* , EventContext eventContext */) {

		logger.info("onEVENT_REPORT_BCSM_REQUEST");
		/*
	        XmlCAPDialog dialog = this.getXmlCAPDialog();
	        dialog.addCAPMessage(((CAPEvent) event).getWrappedEvent());
	        this.setXmlCAPDialog(dialog);

	        camelStatAggregator.updateMessagesRecieved();
	        camelStatAggregator.updateMessagesAll();
		*/
		}


		public void onREQUEST_REPORT_BCSM_EVENT_REQUEST(
				org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.RequestReportBCSMEventRequest event,
				ActivityContextInterface aci/* , EventContext eventContext */) {

		   logger.info("onREQUEST_REPORT_BCSM_EVENT_REQEST");
		if (this.currentCapDialog != null && this.cc != null && this.cc.step != Step.disconnected) {
			this.cc.requestReportBCSMEventRequest = event;
	            	this.bcsmEventList = event.getBCSMEventList();
		}
	        event.getCAPDialog().processInvokeWithoutAnswer(event.getInvokeId());	

		}


		public void onFURNISH_CHARGING_INFORMATION_REQUEST(
				org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.FurnishChargingInformationRequest event,
				ActivityContextInterface aci/* , EventContext eventContext */) {
		}

		public void onPLAY_ANNOUNCEMENT_REQUEST(
				org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.PlayAnnouncementRequest event,
				ActivityContextInterface aci/* , EventContext eventContext */) {
		}

		public void onRELEASE_CALL_REQUEST(
				org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.ReleaseCallRequest event,
				ActivityContextInterface aci/* , EventContext eventContext */) {
		}

		public void onPROMPT_AND_COLLECT_USER_INFORMATION_REQUEST(
				org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.PromptAndCollectUserInformationRequest event,
				ActivityContextInterface aci/* , EventContext eventContext */) {
		}

	    public void onPROMPT_AND_COLLECT_USER_INFORMATION_RESPONSE(
	            org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.PromptAndCollectUserInformationResponse event,
	            ActivityContextInterface aci/* , EventContext eventContext */) {

		/*
	        XmlCAPDialog dialog = this.getXmlCAPDialog();
	        dialog.addCAPMessage(((CAPEvent) event).getWrappedEvent());
	        this.setXmlCAPDialog(dialog);

	        camelStatAggregator.updateMessagesRecieved();
	        camelStatAggregator.updateMessagesAll();
		*/
	    }

		public void onRESET_TIMER_REQUEST(
				org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.ResetTimerRequest event,
				ActivityContextInterface aci/* , EventContext eventContext */) {
		}

		public void onSEND_CHARGING_INFORMATION_REQUEST(
				org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.SendChargingInformationRequest event,
				ActivityContextInterface aci/* , EventContext eventContext */) {
		}

		public void onSPECIALIZED_RESOURCE_REPORT_REQUEST(
				org.mobicents.protocols.ss7.cap.api.service.circuitSwitchedCall.SpecializedResourceReportRequest event,
				ActivityContextInterface aci/* , EventContext eventContext */) {
		}


		public void onREJECT_COMPONENT(org.mobicents.slee.resource.cap.events.RejectComponent event,
				ActivityContextInterface aci/* , EventContext eventContext */) {

		logger.info("onREJECT_COMPONENT");
		/*
	        XmlCAPDialog dialog = this.getXmlCAPDialog();
	        try {
	            dialog.sendRejectComponent(event.getInvokeId(), event.getProblem());
	        } catch (CAPException e) {
	            // This never occur
	            e.printStackTrace();
	        }
	        this.setXmlCAPDialog(dialog);
		*/
		}

		
		public void onERROR_COMPONENT(org.mobicents.slee.resource.cap.events.ErrorComponent event,
				ActivityContextInterface aci/* , EventContext eventContext */) {
		 logger.info("onERROR_COMPONENT");

		/*
	        XmlCAPDialog dialog = this.getXmlCAPDialog();
	        try {
	            dialog.sendErrorComponent(event.getInvokeId(), event.getCAPErrorMessage());
	        } catch (CAPException e) {
	            // This never occur
	            e.printStackTrace();
	        }
	        this.setXmlCAPDialog(dialog);
		*/
		}


		public void onINVOKE_TIMEOUT(org.mobicents.slee.resource.cap.events.InvokeTimeout event,
				ActivityContextInterface aci/* , EventContext eventContext */) {
			logger.info("onINVOKE_TIMEOUT");
		}

		
	
	       
	public void onGet(net.java.slee.resource.http.events.HttpServletRequestEvent event, ActivityContextInterface aci,
		EventContext eventContext) {
	    	logger.info("onGet");
	    	MBeanServer server = MBeanServerLocator.locateJBoss();
	    	
	    	Object obj = null;
	    	if (server != null) {
		    	logger.info("server=" + server);
		    	
    			try {
					obj = server.getAttribute(new ObjectName(
							"org.mobicents.ss7:layer=ALARM,name=AlarmHost,type=Management"), "CurrentAlarmList");
					logger.info("obj=" + obj);
				} catch (AttributeNotFoundException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (InstanceNotFoundException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (MalformedObjectNameException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (MBeanException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (ReflectionException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

    			
    			/*
	    		try {
					obj = server.invoke(new ObjectName(
							"org.mobicents.ss7:layer=ALARM,name=AlarmHost,type=Management"), 
							"getCurrentAlarmList", null, null);
					logger.info("obj=" + obj);
				} catch (InstanceNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (MalformedObjectNameException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ReflectionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (MBeanException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				*/
	    	}
	     //   onRequest(event, aci, eventContext);
	}
	
    public void onPost(net.java.slee.resource.http.events.HttpServletRequestEvent event, ActivityContextInterface aci,
            EventContext eventContext) {
//    	onRequest(event, aci, eventContext);
    }

    
    public void sendInitialDP() throws CAPException {


    	EncodingScheme es = new BCDEvenEncodingScheme();
    	GlobalTitle GTSSF = new GlobalTitle0100Impl(
    			"923330053111", 0, es,
    			org.mobicents.protocols.ss7.indicator.NumberingPlan.ISDN_TELEPHONY,
    		    NatureOfAddress.INTERNATIONAL );
        SccpAddress origAddress = new SccpAddressImpl(RoutingIndicator.ROUTING_BASED_ON_GLOBAL_TITLE, GTSSF,  1, 146);
     	GlobalTitle GTSCF = new GlobalTitle0100Impl(
    			"923330051123", 0, es,
    			org.mobicents.protocols.ss7.indicator.NumberingPlan.ISDN_TELEPHONY,
    		    NatureOfAddress.INTERNATIONAL );

        SccpAddress remoteAddress = new SccpAddressImpl(RoutingIndicator.ROUTING_BASED_ON_DPC_AND_SSN, GTSCF, 2, 146);


        int serviceKey = 1;

        CalledPartyNumber cdpa = this.capProvider.getISUPParameterFactory().createCalledPartyNumber();
        cdpa.setAddress("552348762");
        cdpa.setNatureOfAddresIndicator(NAINumber._NAI_INTERNATIONAL_NUMBER);
        cdpa.setNumberingPlanIndicator(CalledPartyNumber._NPI_ISDN);
        cdpa.setInternalNetworkNumberIndicator(CalledPartyNumber._INN_ROUTING_ALLOWED);
        CalledPartyNumberCap calledPartyNumber=null;
		try {
			calledPartyNumber = this.capProvider.getCAPParameterFactory()
			        .createCalledPartyNumberCap(cdpa);
		} catch (CAPException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        CallingPartyNumber cgpa = this.capProvider.getISUPParameterFactory().createCallingPartyNumber();
        cgpa.setAddress("55998223");
        cgpa.setNatureOfAddresIndicator(NAINumber._NAI_INTERNATIONAL_NUMBER);
        cgpa.setNumberingPlanIndicator(CalledPartyNumber._NPI_ISDN);
        cgpa.setAddressRepresentationREstrictedIndicator(CallingPartyNumber._APRI_ALLOWED);
        cgpa.setScreeningIndicator(CallingPartyNumber._SI_NETWORK_PROVIDED);
        CallingPartyNumberCap callingPartyNumber = null;
		try {
			callingPartyNumber = this.capProvider.getCAPParameterFactory()
			        .createCallingPartyNumberCap(cgpa);
		} catch (CAPException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


        LocationNumber locNum = this.capProvider.getISUPParameterFactory()
				.createLocationNumber();
        locNum.setAddress("55200001");
        locNum.setNatureOfAddresIndicator(NAINumber._NAI_INTERNATIONAL_NUMBER);
        locNum.setNumberingPlanIndicator(LocationNumber._NPI_ISDN);
        locNum.setAddressRepresentationRestrictedIndicator(LocationNumber._APRI_ALLOWED);
        locNum.setScreeningIndicator(LocationNumber._SI_NETWORK_PROVIDED);
        locNum.setInternalNetworkNumberIndicator(LocationNumber._INN_ROUTING_ALLOWED);
        LocationNumberCap locationNumber=null;
		try {
			locationNumber = this.capProvider.getCAPParameterFactory()
			        .createLocationNumberCap(locNum);
		} catch (CAPException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        ISDNAddressString vlrNumber = this.capProvider.getMAPParameterFactory()
                .createISDNAddressString(AddressNature.international_number, NumberingPlan.ISDN, "552000002");
        LocationInformation locationInformation = this
                .capProvider
                .getMAPParameterFactory()
                .createLocationInformation(10, null, vlrNumber, null, null, null, null, vlrNumber, null, false, false, null,
                        null);


        EventTypeBCSM eventTypeBCSM = EventTypeBCSM.collectedInfo;

        // First create Dialog

        CAPApplicationContext acn = CAPApplicationContext.CapV2_gsmSSF_to_gsmSCF;
        CAPDialogCircuitSwitchedCall capDialog = this.capProvider.getCAPServiceCircuitSwitchedCall()
        		.createNewDialog(acn, origAddress, remoteAddress);
        capDialog = capProvider.getCAPServiceCircuitSwitchedCall().createNewDialog(acn, origAddress, remoteAddress);

        capDialog.addInitialDPRequest(serviceKey, calledPartyNumber, callingPartyNumber, null, null, null,
                locationNumber, null, null, null, null, null, eventTypeBCSM, null, null, null, null, null, null, null, false,
                null, null, locationInformation, null, null, null, null, null, false, null);

        if (this.currentCapDialog == null)
        {
        	this.currentCapDialog = capDialog;
        	this.cc = new CallContent();
        	this.cc.step = Step.initialDPSent;
        	this.cc.calledPartyNumber = calledPartyNumber;
        	this.cc.callingPartyNumber = callingPartyNumber;
        	this.capAcif.getActivityContextInterface(capDialog).attach(this.sbbContext.getSbbLocalObject());

        	// This will initiate the TC-BEGIN with INVOKE component
        	capDialog.send();
	}


   }    

    public void sendEventReportBCSM_OCalledPartyBusy() throws CAPException {
    	
  	    	 	
        if (currentCapDialog != null && this.cc != null) {
        	
      		// call goes busy
    		CauseIndicators busycauseIndicators = this.capProvider
    		.getISUPParameterFactory().createCauseIndicators();
    		//    causeIndicators.setLocation(CauseIndicators._LOCATION_USER);
    		busycauseIndicators.setCodingStandard(CauseIndicators._CODING_STANDARD_ITUT);
    		busycauseIndicators.setCauseValue(CauseIndicators._CV_USER_BUSY);
    		CauseCap busyCause = this.capProvider
    				.getCAPParameterFactory().createCauseCap(busycauseIndicators);

    		OCalledPartyBusySpecificInfo oCalledPartyBusySpecificInfo = this.capProvider
        		.getCAPParameterFactory()
        		.createOCalledPartyBusySpecificInfo(busyCause);
            ReceivingSideID legID = this.capProvider
            		.getCAPParameterFactory().createReceivingSideID(LegType.leg2);
            MiscCallInfo miscCallInfo = this.capProvider
            		.getINAPParameterFactory()
                    .createMiscCallInfo(MiscCallInfoMessageType.notification, null);

        	
            EventSpecificInformationBCSM eventSpecificInformationBCSM = this.capProvider
            		.getCAPParameterFactory()
                    .createEventSpecificInformationBCSM(oCalledPartyBusySpecificInfo);
            currentCapDialog.addEventReportBCSMRequest(EventTypeBCSM.oCalledPartyBusy, 
            		eventSpecificInformationBCSM, legID,
                    miscCallInfo, null);
            currentCapDialog.send();
            this.cc.step = Step.calledbusy;
        }
    }
    
    
    public void sendEventReportBCSM_ONoAnswer() throws CAPException {
        if (currentCapDialog != null && this.cc != null) {
        	
    		ONoAnswerSpecificInfo oNoAnswerSpecificInfo = this.capProvider.getCAPParameterFactory()
	        		.createONoAnswerSpecificInfo();
    		ReceivingSideID legID = this.capProvider.getCAPParameterFactory().createReceivingSideID(LegType.leg2);
    		MiscCallInfo miscCallInfo = this.capProvider.getINAPParameterFactory()
            .createMiscCallInfo(MiscCallInfoMessageType.notification, null);
	        

            EventSpecificInformationBCSM eventSpecificInformationBCSM = this.capProvider.getCAPParameterFactory()
                    .createEventSpecificInformationBCSM(oNoAnswerSpecificInfo);
            currentCapDialog.addEventReportBCSMRequest(EventTypeBCSM.oNoAnswer, eventSpecificInformationBCSM, legID,
                    miscCallInfo, null);
            currentCapDialog.send();
            this.cc.step = Step.notanswered;
        }
    }



    public void sendEventReportBCSM_OAbandon() throws CAPException {
        if (currentCapDialog != null && this.cc != null) {
        	
            OAbandonSpecificInfo oAbandonSpecificInfo = this.capProvider
            		.getCAPParameterFactory()
                    .createOAbandonSpecificInfo(true);
            MiscCallInfo miscCallInfo = this.capProvider.getINAPParameterFactory()
                    .createMiscCallInfo(MiscCallInfoMessageType.notification, null);

            EventSpecificInformationBCSM eventSpecificInformationBCSM = this.capProvider
            		.getCAPParameterFactory()
                    .createEventSpecificInformationBCSM(oAbandonSpecificInfo);
            currentCapDialog.addEventReportBCSMRequest(EventTypeBCSM.oAbandon, 
            		eventSpecificInformationBCSM, null, miscCallInfo, null);
            currentCapDialog.send();
            this.cc.step = Step.oabandon;
        }
    }
    
    
    public void sendEventReportBCSM_RouteSelectFailure() throws CAPException {
        if (currentCapDialog != null && this.cc != null) {
        	
        	
    		CauseIndicators causeIndicators = this.capProvider.getISUPParameterFactory()
    				.createCauseIndicators();
    		causeIndicators.setLocation(CauseIndicators._LOCATION_USER);
    		causeIndicators.setCodingStandard(CauseIndicators._CODING_STANDARD_ITUT);
    		causeIndicators.setCauseValue(CauseIndicators._CV_NO_ROUTE_TO_DEST);
    		CauseCap releaseCause = this.capProvider.getCAPParameterFactory()
    				.createCauseCap(causeIndicators);
    		RouteSelectFailureSpecificInfo routeSelectFailureSpecificInfo = this.capProvider
    				.getCAPParameterFactory()
                .createRouteSelectFailureSpecificInfo(releaseCause);
    		 MiscCallInfo miscCallInfo = this.capProvider.getINAPParameterFactory()
            .createMiscCallInfo(MiscCallInfoMessageType.notification, null);

        	
            EventSpecificInformationBCSM eventSpecificInformationBCSM = this.capProvider
            		.getCAPParameterFactory()
                    .createEventSpecificInformationBCSM(routeSelectFailureSpecificInfo);
            currentCapDialog.addEventReportBCSMRequest(EventTypeBCSM.routeSelectFailure, 
            		eventSpecificInformationBCSM, null, miscCallInfo, null);
            currentCapDialog.send();
            this.cc.step = Step.routeselectfailure;
        }
    }

    public void sendEventReportBCSM_OAnswer() throws CAPException {
        if (currentCapDialog != null && this.cc != null) {
        	
    		OAnswerSpecificInfo oAnswerSpecificInfo = this.capProvider.getCAPParameterFactory()
    				.createOAnswerSpecificInfo(null, false, false, null, null, null);
    		ReceivingSideID legID = this.capProvider
        		.getCAPParameterFactory().createReceivingSideID(LegType.leg2);
    		MiscCallInfo miscCallInfo = this.capProvider.getINAPParameterFactory()
        		.createMiscCallInfo(MiscCallInfoMessageType.notification, null);

            EventSpecificInformationBCSM eventSpecificInformationBCSM = this.capProvider.getCAPParameterFactory()
                    .createEventSpecificInformationBCSM(oAnswerSpecificInfo);
            currentCapDialog.addEventReportBCSMRequest(EventTypeBCSM.oAnswer, eventSpecificInformationBCSM, legID,
                    miscCallInfo, null);
            currentCapDialog.send();
            this.cc.step = Step.answered;
        }
    }
    
    
    public void sendEventReportBCSM_ODisconnect() throws CAPException {
    	
    	

        if (currentCapDialog != null && this.cc != null) {
        	
    		CauseIndicators causeIndicators = this.capProvider
    				.getISUPParameterFactory().createCauseIndicators();
    		causeIndicators.setLocation(CauseIndicators._LOCATION_USER);
    		causeIndicators.setCodingStandard(CauseIndicators._CODING_STANDARD_ITUT);
    		causeIndicators.setCauseValue(CauseIndicators._CV_ALL_CLEAR);
    		CauseCap releaseCause = this.capProvider
    				.getCAPParameterFactory().createCauseCap(causeIndicators);
    		ODisconnectSpecificInfo oDisconnectSpecificInfo = this.capProvider.getCAPParameterFactory()
                .createODisconnectSpecificInfo(releaseCause);
    		ReceivingSideID legID = this.capProvider.getCAPParameterFactory()
    				.createReceivingSideID(LegType.leg2);
    		MiscCallInfo miscCallInfo = this.capProvider.getINAPParameterFactory()
                .createMiscCallInfo(MiscCallInfoMessageType.notification, null);

            EventSpecificInformationBCSM eventSpecificInformationBCSM = this.capProvider.getCAPParameterFactory()
                    .createEventSpecificInformationBCSM(oDisconnectSpecificInfo);
            currentCapDialog.addEventReportBCSMRequest(EventTypeBCSM.oDisconnect, eventSpecificInformationBCSM, legID,
                    miscCallInfo, null);
            currentCapDialog.send();
            this.cc.step = Step.disconnected;
        }
    }

    
	private void sendCamelOperation(String camelop) {


        try {

        	
        	if (camelop.equalsIgnoreCase("idp") ) {
            	this.sendInitialDP();

        	} 
        	else if (camelop.equalsIgnoreCase("busy") ) {
            	this.sendEventReportBCSM_OCalledPartyBusy();  		
        	}
        	else if (camelop.equalsIgnoreCase("noanswer") ) {
            	this.sendEventReportBCSM_ONoAnswer();  		
        	}
        	else if (camelop.equalsIgnoreCase("abandon") ) {
            	this.sendEventReportBCSM_OAbandon(); 		
        	}
        	else if (camelop.equalsIgnoreCase("routefailure") ) {
            	this.sendEventReportBCSM_RouteSelectFailure();
        	}
        	else if (camelop.equalsIgnoreCase("answer") ) {
            	this.sendEventReportBCSM_OAnswer();  		
        	}
        	else if (camelop.equalsIgnoreCase("disconnect") ) {
            	this.sendEventReportBCSM_ODisconnect();		
        	}
        	
		} catch (CAPException e) {
			
			logger.severe("We GOT ERROR in sending request");
			e.printStackTrace();
		}

	}



	
    private void onRequest(net.java.slee.resource.http.events.HttpServletRequestEvent event, ActivityContextInterface aci,
            EventContext eventContext) {
    	setEventContextCMP(eventContext);
    	HttpServletRequest httpServletRequest = event.getRequest();
    	HttpRequestType httpRequestType = HttpRequestType.fromPath(httpServletRequest.getPathInfo());

    	//logger.info("httpServletRequest.getPathInfo()="+httpServletRequest.getPathInfo());
    	setHttpRequest(new HttpRequest(httpRequestType));


    	/*
    	try {
    		java.io.BufferedReader rd = httpServletRequest.getReader();
//    		logger.info("rd=" + rd);

    		String rline = "";
    		while ((rline = rd.readLine()) != null) {
    			logger.info(rline);
    		} 
    	} catch (Exception e) {
    			logger.severe("Exception while getting params ", e);
    	}
    	*/

    	logger.info("request=" + httpServletRequest);
    	logger.info("httpRequestType="+httpRequestType);
    	String camelop = null;
    	logger.info("httpServletRequest.getQueryString()="+httpServletRequest.getQueryString());
    	switch (httpRequestType) {
    		case REST:
    			camelop = httpServletRequest.getParameter("camelop");
    			break;
    		default:
    			sendHTTPResult(HttpServletResponse.SC_NOT_FOUND, "Request URI unsupported");
    			return;
    	}


    	setHttpRequest(new HttpRequest(httpRequestType, null));
    	if (logger.isInfoEnabled()){
    		logger.info(String.format("Handling %s request, operation: %s", httpRequestType.name().toUpperCase(), camelop));
    	}

    	if (camelop != null) {
    		switch(httpRequestType) {
    			case REST:
    					//eventContext.suspendDelivery();
    					sendCamelOperation(camelop);
    					break;
    		}

    	} else {
    		logger.info("MSISDN is null, sending back -1 for Global Cell Identity");
    		handleLocationResponse( "Invalid MSISDN specified", null);
    	}
}

    

    /**
     * Handle generating the appropriate HTTP response
     * We're making use of the MLPResponse class for both GET/POST requests for convenience and
     * because eventually the GET method will likely be removed
     * @param mlpResultType OK or error type to return to client
     * @param response CGIResponse on location attempt
     * @param mlpClientErrorMessage Error message to send to client
     */
    
    private void handleLocationResponse( String response, String mlpClientErrorMessage) {
        HttpRequest request = getHttpRequest();

                	StringBuilder getResponse = new StringBuilder();

	//		getResponse.append("text=");
			if (response != null)
			getResponse.append(response);


                    this.sendHTTPResult(HttpServletResponse.SC_OK, getResponse.toString());
    }

    /**
     * Return the specified response data to the HTTP client
     * @param responseData Response data to send to client
     */
	
	private void sendHTTPResult(int statusCode, String responseData) {
		try {
			EventContext ctx = this.getEventContextCMP();
            if (ctx == null) {
                if (logger.isWarningEnabled()) {
                    logger.warning("When responding to HTTP no pending HTTP request is found, responseData=" + responseData);
                    return;
                }
            }

	        HttpServletRequestEvent event = (HttpServletRequestEvent) ctx.getEvent();

			HttpServletResponse response = event.getResponse();
                        response.setStatus(statusCode);
            PrintWriter w = null;
            w = response.getWriter();
            w.print(responseData);
			w.flush();
			response.flushBuffer();

			if (ctx.isSuspended()) {
				ctx.resumeDelivery();
			}

			if (logger.isInfoEnabled()){
			    logger.info("HTTP Request received and response sent, responseData=" + responseData);
			}

			// getNullActivity().endActivity();
		} catch (Exception e) {
			logger.severe("Error while sending back HTTP response", e);
		}
	}



    private void endHttpSessionActivity() {
        HttpSessionActivity httpSessionActivity = this.getHttpSessionActivity();
        if (httpSessionActivity != null) {
            httpSessionActivity.endActivity();
        }
    }


    private HttpSessionActivity getHttpSessionActivity() {
        ActivityContextInterface[] acis = this.sbbContext.getActivities();
        for (ActivityContextInterface aci : acis) {
            Object activity = aci.getActivity();
            if (activity instanceof HttpSessionActivity) {
                return (HttpSessionActivity) activity;
            }
        }
        return null;
    }






  
  

  private EventContext resumeHttpEventContext() {
        EventContext httpEventContext = getEventContextCMP();

        if (httpEventContext == null) {
            logger.severe("No HTTP event context, can not resume ");
            return null;
        }

        httpEventContext.resumeDelivery();
        return httpEventContext;
    }


  private void sendHttpResponse() {
            if (logger.isFineEnabled())
                logger.fine("About to send HTTP response.");

            XmlCAPDialog dialog = getXmlCAPDialog();
            
            
            byte[] data=null;
			try {
				data = getEventsSerializeFactory().serialize(dialog);
			} catch (javolution.xml.stream.XMLStreamException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

            if (logger.isFineEnabled()) {
                logger.fine("Sending HTTP Response Payload = \n" + new String(data));
            }

            EventContext httpEventContext = this.resumeHttpEventContext();

            if (httpEventContext == null) {
                 // TODO: terminate dialog?
                logger.severe("No HTTP event context, can not deliver response for MapXmlDialog: " + dialog);
                return;
            }

	        try {
            HttpServletRequestEvent httpRequest = (HttpServletRequestEvent) httpEventContext.getEvent();
            HttpServletResponse response = httpRequest.getResponse();
            response.setStatus(HttpServletResponse.SC_OK);
            try {
                response.getOutputStream().write(data);
                response.getOutputStream().flush();
            } catch (NullPointerException npe) {
                logger.warning(  "Probably HTTPResponse already sent by HTTP-Servlet-RA. Increase HTTP_REQUEST_TIMEOUT in deploy-config.xml of RA to be greater than TCAP Dialog timeout", npe);
            }

  //      } catch (XMLStreamException xmle) {
    //        logger.severe("Failed to serialize dialog", xmle);
        } catch (IOException e) {
            logger.severe("Failed to send answer!", e);
        }

    }


    
    /**
	 * CMP
	 */

	public abstract void setXmlCAPDialog(XmlCAPDialog dialog);
	public abstract XmlCAPDialog getXmlCAPDialog();


	public abstract void setEventContextCMP(EventContext eventContext);
	public abstract EventContext getEventContextCMP();


    public abstract void setHttpRequest(HttpRequest httpRequest);
    public abstract HttpRequest getHttpRequest();

    
    
    /**
     * HTTP Request Types (GET)
     */
    private enum HttpRequestType {
        REST("rest"),
        UNSUPPORTED("404");

        private String path;

        HttpRequestType(String path) {
            this.path = path;
        }

        public String getPath() {
            return String.format("/ssp/%s", path);
        }

        public static HttpRequestType fromPath(String path) {
            for (HttpRequestType type: values()) {
                if (path.equals(type.getPath())) {
                    return type;
                }
            }

            return UNSUPPORTED;
        }
    }

    /**
     * Request
     */
    private class HttpRequest implements Serializable {
        HttpRequestType type;
        String msisdn;

        public HttpRequest(HttpRequestType type, String msisdn) {
            this.type = type;
            this.msisdn = msisdn;
        }

        public HttpRequest(HttpRequestType type) {
            this(type, "");
        }
    }

	public enum Step {
        initialDPSent, callAllowed, answered, disconnected, notanswered, calledbusy, oabandon, routeselectfailure;
    }

    public class CallContent {
        public Step step;
        public Long activityTestInvokeId;

        public CalledPartyNumberCap calledPartyNumber;
        public CallingPartyNumberCap callingPartyNumber;
        public RequestReportBCSMEventRequest requestReportBCSMEventRequest;
        public DestinationRoutingAddress destinationRoutingAddress;
    }
	
 

}
