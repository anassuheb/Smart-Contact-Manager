package com.smart.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

import org.aspectj.weaver.NewConstructorTypeMunger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.smart.dao.ContactRepository;
import com.smart.dao.UserRepository;
import com.smart.entities.Contact;
import com.smart.entities.User;
import com.smart.helper.Message;

import jakarta.servlet.http.HttpSession;


@Controller
@RequestMapping("/user")
public class UserController {
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private ContactRepository contactRepository;
	
	//method for adding common data to response
	@ModelAttribute
	public void addCommonData(Model model,Principal principal) {
		
		String userName = principal.getName();
		System.out.println("USERNAME "+userName);
		
		//get user using username or email
		
		User user = userRepository.getUserByUserName(userName);
		
		System.out.println("USER "+user);
		
		model.addAttribute("user",user);
		
	}

	//dashboard home
	@RequestMapping("/index")
	public String dashboard(Model model,Principal principal) {
		model.addAttribute("title", "User Dashboard");
		return "normal/user_dashboard";
		
	}
	
	// open add form handler
	
	@GetMapping("/add-contact")
	public String openAddContactForm(Model model) {
		model.addAttribute("title", "Add Contact");
		model.addAttribute("contact", new Contact());
		return "normal/add_contact_form";
	}
	
	//processing add contact form
	
	@PostMapping("/process-contact")
	public String processContact(
			@ModelAttribute Contact contact,
			@RequestParam("profileImage") MultipartFile file, 
			Principal principal) {
		
		try {
			
		String name = principal.getName();
		User user = this.userRepository.getUserByUserName(name);
		
		contact.setUser(user);	
		
		if(file.isEmpty()) {
			System.out.println("file is empty");
			contact.setImage("contact.png");
		}else {
			//upload file to folder and update name to contact
			contact.setImage(file.getOriginalFilename());
			File saveFile = new ClassPathResource("static/img").getFile();
			Path path = Paths.get(saveFile.getAbsolutePath()+File.separator+file.getOriginalFilename());
			Files.copy(file.getInputStream(),path, StandardCopyOption.REPLACE_EXISTING);
			System.out.println("image is uploaded");
		}
		
		user.getContacts().add(contact);
		
		
		
		this.userRepository.save(user);
		System.out.println("DATA "+contact);
		System.out.println("added to data base");
		
		
		
		
		}catch (Exception e) {
			System.out.println("ERROR"+e.getMessage());
			e.printStackTrace();
			
		}
		
		return "normal/add_contact_form";
	} 
	
	@GetMapping("/show-contacts/{page}")
	public String showContacts(@PathVariable("page") Integer page,Model m,Principal principal) {
		m.addAttribute("title", "View Contacts");
		//send contact list from here
		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);
		
		
		//current page=page
		//conatct per page=5
		Pageable pageable = PageRequest.of(page,4);
		Page<Contact> contacts = this.contactRepository.findContactsByUser(user.getId(),pageable);
		
		m.addAttribute("contacts",contacts);
		m.addAttribute("currentPage",page);
		m.addAttribute("totalPages",contacts.getTotalPages());
		
		return "normal/show_contacts";
	}
	
	//showing specific contact detail
	
	@GetMapping("/{cId}/contact")
	public String showConatctDetail(@PathVariable("cId") Integer cId,Model model,Principal principal) {
		System.out.println(cId);
		Optional<Contact> contactOptional = this.contactRepository.findById(cId);
		Contact contact = contactOptional.get();
		
		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);
		
		if(user.getId()==contact.getUser().getId()) {
			model.addAttribute("contact", contact);	
		}
		return"/normal/contact_details";
	}
	
	//delete contact handler
	
	@GetMapping("/delete/{cid}")
	public String deleteContact(@PathVariable("cid") Integer cId,Model model,HttpSession session) 
	{
		
		Optional<Contact> contOptional = this.contactRepository.findById(cId);
		Contact contact = contOptional.get();
		
		contact.setUser(null);
		this.contactRepository.delete(contact);
		
		session.setAttribute("message", new Message("Contact deleted Successfully" ,"success"));
		
		return"redirect:/user/show-contacts/0";
		}
	
	
}
