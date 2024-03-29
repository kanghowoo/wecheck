package org.afive.wecheck.user.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import org.afive.wecheck.configuration.ResponseCode;
import org.afive.wecheck.res.FilePathResource;
import org.afive.wecheck.service.FileBean;
import org.afive.wecheck.service.FileService;
import org.afive.wecheck.user.bean.AccessTokenBean;
import org.afive.wecheck.user.bean.ConfirmRequestBean;
import org.afive.wecheck.user.mapper.AccessTokenMapper;
import org.afive.wecheck.user.mapper.ConfirmRequestMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(value = "requests")
public class ConfirmRequestController {
	
	@Autowired
	private AccessTokenMapper accessTokenMapper;
	
	@Autowired
	private ConfirmRequestMapper confirmRequestMapper;
	
	@RequestMapping(value="", method = RequestMethod.POST, consumes = { "multipart/form-data" }, headers = "content-type=application/x-www-form-urlencoded")
	private Map<String, Object> register(
					@RequestHeader("Authorization") String accessToken, 
					@RequestParam("firstName")String firstName,
					@RequestParam("lastName")String lastName,
					@RequestParam("gender")String intGender,
					@RequestParam("regionID")String intRegionID,
					@RequestParam("unitID")String intUnitID,
					@RequestParam("birthDay")String birthDay,
					@RequestParam(value="profileImage", required=false)MultipartFile profileImageFile
					) {
				
		int gender= Integer.parseInt(intGender);
		int regionID=Integer.parseInt(intRegionID);
		int unitID = Integer.parseInt(intUnitID);
		
		Map<String, Object> result=new HashMap<String, Object>();
		System.out.println("result 객체 생성..");
		
		System.out.println("넣어둔 accessToken : "+accessToken);
		AccessTokenBean accessTokenBean = accessTokenMapper.get(accessToken);
		
		System.out.println("accessToken가져왔다.");
		
		//잘못된 accessToken!
		if(accessTokenBean==null) {
			System.out.println("accessToken null이다.");
			result.put("responseCode", String.valueOf(ResponseCode.ACCESS_DENIED_WRONG_ACCESSCODE));
			return result;
		}
		
		System.out.println("accessToken 잘 있다."+accessTokenBean.getAccessTokenID());
		
		Integer confirmRequestID=accessTokenBean.getConfirmRequestID();
		
		System.out.println("confirmRequestID : "+confirmRequestID);
		
		ConfirmRequestBean confirmRequestBean;
		
		/**
    	 * 2019-01-17 edited by gangho - 시간등록 DB now() 가 아닌 자바에서 등록
    	 */
    	String localDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
		
		//confirmRequest가 존재하
		if(confirmRequestID>0) {
			confirmRequestBean=confirmRequestMapper.get(confirmRequestID+"");
			confirmRequestBean.updateValues(firstName, lastName, gender, regionID, unitID, birthDay);
			
			/**
	    	 * 2019-01-17 edited by gangho - 시간등록 DB now() 가 아닌 자바에서 등록
	    	 */
			confirmRequestBean.setRequestedTime(localDateTime);
			confirmRequestMapper.update(confirmRequestBean);
			
			System.out.println("confirmRequest 수정, confirmRequestID : "+accessTokenBean.getConfirmRequestID());
			
			if(profileImageFile!=null) {
				MultipartFile imageFile = profileImageFile;
				
				//사진을 새로 넣은 경
				if(!imageFile.isEmpty()) {
					FileService fs = new FileService();
					
					
					//원래 프사 있는지 확인
					if(confirmRequestBean.getCrProfileImage()!=null && confirmRequestBean.getCrProfileImage().length()>0) {
						
						System.out.println("원래 있던 프사 지울거에염! 프사 경로 : "+confirmRequestBean.getCrProfileImage());
						
						if(fs.fileDelete(confirmRequestBean.getCrProfileImage())) {
							System.out.println("삭제성공");
						}else {
							System.out.println("삭제 실패");
						}
						
					}
					
					System.out.println("imageFile들어있다.");
					
					System.out.println("파일명 : " + imageFile.getOriginalFilename());
					
					try {
						FileBean fileBean = fs.fileUpload(imageFile, FilePathResource.PROFILE_IMAGE_PATH, String.valueOf(confirmRequestBean.getConfirmRequestID()));
						
						Map<String, String> inputMap = new HashMap<String, String>();
						inputMap.put("confirmRequestID", String.valueOf(confirmRequestBean.getConfirmRequestID()));
						inputMap.put("crProfileImage", fileBean.getFilePath());

						
						confirmRequestMapper.updateImageFile(inputMap);
						confirmRequestBean.setCrProfileImage(fileBean.getFilePath());
						
						result.put("confirmRequest", confirmRequestBean);
				
						result.put("responseCode", ResponseCode.SUCCESS);
					} catch (Exception e) {
						e.printStackTrace();
						
						result.put("confirmRequest", confirmRequestBean);
						
						result.put("responseCode", ResponseCode.FAILED_FILE_UPLOAD);
				
						
						new ResponseEntity<Object>(confirmRequestBean, HttpStatus.NO_CONTENT); 
					}
					
				}else {
					result.put("responseCode", ResponseCode.FAILED_FILE_NOT_FOUND);
					result.put("confirmRequest", confirmRequestBean);
				}
			}else {
				result.put("responseCode", ResponseCode.SUCCESS);
				result.put("confirmRequest", confirmRequestBean);
			}
			
			System.out.println("confirmRequest 객체 생성..!");
			
			
		}else {
			confirmRequestBean=new ConfirmRequestBean(accessTokenBean.getSnsLoginID(), firstName, lastName, gender, regionID, unitID, birthDay);
			/**
	    	 * 2019-01-17 edited by gangho - 시간등록 DB now() 가 아닌 자바에서 등록
	    	 */
			confirmRequestBean.setRequestedTime(localDateTime);
			
			confirmRequestMapper.register(confirmRequestBean);
			
			
			System.out.println("confirmRequest 새로 등 아이디 : "+confirmRequestBean.getConfirmRequestID());
			
			accessTokenBean.setConfirmRequestID(confirmRequestBean.getConfirmRequestID());
			accessTokenMapper.updateConfirmRequestID(accessTokenBean);
			
			MultipartFile imageFile = profileImageFile;
			
			if(!imageFile.isEmpty()) {
				
				System.out.println("imageFile들어있다.");
				
				System.out.println("파일명 : " + imageFile.getOriginalFilename());
				FileService fs = new FileService();
				try {
					FileBean fileBean = fs.fileUpload(imageFile, FilePathResource.PROFILE_IMAGE_PATH, String.valueOf(confirmRequestBean.getConfirmRequestID()));
					
					Map<String, String> inputMap = new HashMap<String, String>();
					inputMap.put("confirmRequestID", String.valueOf(confirmRequestBean.getConfirmRequestID()));
					inputMap.put("crProfileImage", fileBean.getFilePath());

					
					confirmRequestMapper.updateImageFile(inputMap);
					confirmRequestBean.setCrProfileImage(fileBean.getFilePath());
					
					result.put("confirmRequest", confirmRequestBean);
			
					result.put("responseCode", ResponseCode.SUCCESS);
				} catch (Exception e) {
					e.printStackTrace();
					
					result.put("confirmRequest", confirmRequestBean);
					
					result.put("responseCode", ResponseCode.FAILED_FILE_UPLOAD);
					
					new ResponseEntity<Object>(confirmRequestBean, HttpStatus.NO_CONTENT); 
				}
				
			}else {
				result.put("responseCode", result.put("responseCode", ResponseCode.FAILED_FILE_NOT_FOUND));
			}
			
			System.out.println("confirmRequest 객체 생성..!");
			
			
		}		
		
		
		
		
		return result;
		
	}
	
	
	
	
}
